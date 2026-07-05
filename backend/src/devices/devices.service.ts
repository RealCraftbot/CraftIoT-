import { Injectable, NotFoundException, ForbiddenException, ConflictException, Inject, forwardRef } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateDeviceDto } from './dto/create-device.dto';
import { ShareDeviceDto } from './dto/share-device.dto';
import { ControlDeviceDto } from './dto/control-device.dto';
import { SharePermission, Role } from '@prisma/client';
import { MqttService } from '../mqtt/mqtt.service';

@Injectable()
export class DevicesService {
  constructor(
    private prisma: PrismaService,
    @Inject(forwardRef(() => MqttService))
    private mqttService: MqttService,
  ) {}

  async create(dto: CreateDeviceDto, userId: string) {
    const existingDevice = await this.prisma.ioTDevice.findUnique({
      where: { id: dto.id },
    });
    if (existingDevice) {
      throw new ConflictException(`Device with ID ${dto.id} already registered`);
    }

    const existingMac = await this.prisma.ioTDevice.findUnique({
      where: { macAddress: dto.macAddress },
    });
    if (existingMac) {
      throw new ConflictException(`Device with MAC Address ${dto.macAddress} already exists`);
    }

    const device = await this.prisma.ioTDevice.create({
      data: {
        id: dto.id,
        name: dto.name,
        type: dto.type,
        ipAddress: dto.ipAddress,
        macAddress: dto.macAddress,
        connectionType: dto.connectionType || 'WIFI',
        owners: {
          create: {
            userId: userId,
          },
        },
      },
    });

    // Automatically subscribe to this device's MQTT topic
    this.mqttService.subscribeToDevice(device.id);

    return device;
  }

  async findAll(user: any) {
    if (user.role === Role.ADMIN) {
      return this.prisma.ioTDevice.findMany({
        include: {
          owners: { include: { user: { select: { fullName: true, email: true } } } },
          shares: { include: { user: { select: { fullName: true, email: true } } } },
        },
      });
    }

    // Normal users see owned devices + shared devices
    const owned = await this.prisma.ioTDevice.findMany({
      where: {
        owners: { some: { userId: user.id } },
      },
      include: {
        shares: { include: { user: { select: { fullName: true, email: true } } } },
      },
    });

    const shared = await this.prisma.ioTDevice.findMany({
      where: {
        shares: { some: { userId: user.id } },
      },
      include: {
        shares: {
          where: { userId: user.id },
          select: { permission: true },
        },
      },
    });

    return { owned, shared };
  }

  async findOne(id: string, user: any) {
    const device = await this.prisma.ioTDevice.findUnique({
      where: { id },
      include: {
        owners: true,
        shares: true,
      },
    });

    if (!device) {
      throw new NotFoundException(`Device with ID ${id} not found`);
    }

    // Verify access permission
    if (user.role !== Role.ADMIN) {
      const isOwner = device.owners.some((o) => o.userId === user.id);
      const isShared = device.shares.some((s) => s.userId === user.id);
      if (!isOwner && !isShared) {
        throw new ForbiddenException('You do not have permission to view this device');
      }
    }

    return device;
  }

  async share(deviceId: string, dto: ShareDeviceDto, currentUser: any) {
    const device = await this.prisma.ioTDevice.findUnique({
      where: { id: deviceId },
      include: { owners: true },
    });

    if (!device) {
      throw new NotFoundException(`Device with ID ${deviceId} not found`);
    }

    // Verify current user owns the device (or is Admin)
    const isOwner = device.owners.some((o) => o.userId === currentUser.id);
    if (!isOwner && currentUser.role !== Role.ADMIN) {
      throw new ForbiddenException('Only the device owner can share this device');
    }

    // Find recipient
    const recipient = await this.prisma.user.findUnique({
      where: { email: dto.email },
    });

    if (!recipient) {
      throw new NotFoundException(`User with email ${dto.email} not found`);
    }

    // Do not share with oneself
    if (recipient.id === currentUser.id) {
      throw new ConflictException('You cannot share a device with yourself');
    }

    // Upsert share record
    return this.prisma.deviceShare.upsert({
      where: {
        deviceId_userId: {
          deviceId,
          userId: recipient.id,
        },
      },
      update: {
        permission: dto.permission,
      },
      create: {
        deviceId,
        userId: recipient.id,
        permission: dto.permission,
      },
    });
  }

  async unshare(deviceId: string, userId: string, currentUser: any) {
    const device = await this.prisma.ioTDevice.findUnique({
      where: { id: deviceId },
      include: { owners: true },
    });

    if (!device) {
      throw new NotFoundException(`Device with ID ${deviceId} not found`);
    }

    // Verify current user owns the device, is Admin, or is removing themselves
    const isOwner = device.owners.some((o) => o.userId === currentUser.id);
    if (!isOwner && currentUser.role !== Role.ADMIN && currentUser.id !== userId) {
      throw new ForbiddenException('You do not have permission to modify sharing for this device');
    }

    await this.prisma.deviceShare.delete({
      where: {
        deviceId_userId: {
          deviceId,
          userId,
        },
      },
    });

    return { success: true, message: 'Device access unshared successfully' };
  }

  async control(id: string, dto: ControlDeviceDto, currentUser: any) {
    const device = await this.findOne(id, currentUser);

    // Verify Write permission
    if (currentUser.role !== Role.ADMIN) {
      const isOwner = device.owners.some((o) => o.userId === currentUser.id);
      const share = device.shares.find((s) => s.userId === currentUser.id);
      const hasWriteAccess = share && (share.permission === SharePermission.WRITE || share.permission === SharePermission.ALL);

      if (!isOwner && !hasWriteAccess) {
        throw new ForbiddenException('You do not have control (write) permission for this device');
      }
    }

    // Update state flags/values in DB
    const updatedDevice = await this.prisma.ioTDevice.update({
      where: { id },
      data: {
        ...(dto.stateFlag1 !== undefined && { stateFlag1: dto.stateFlag1 }),
        ...(dto.sensorValue1 !== undefined && { sensorValue1: dto.sensorValue1 }),
        ...(dto.sensorValue2 !== undefined && { sensorValue2: dto.sensorValue2 }),
        lastActive: new Date(),
      },
    });

    // Construct command payload
    const payload = {
      deviceId: id,
      stateFlag1: updatedDevice.stateFlag1,
      sensorValue1: updatedDevice.sensorValue1,
      sensorValue2: updatedDevice.sensorValue2,
      timestamp: new Date().toISOString(),
    };

    // Publish MQTT Control Command
    this.mqttService.publishToDevice(id, 'control', payload);

    // Log telemetry actions
    if (dto.sensorValue1 !== undefined) {
      await this.logSensor(id, 'sensorValue1', dto.sensorValue1);
    }
    if (dto.sensorValue2 !== undefined) {
      await this.logSensor(id, 'sensorValue2', dto.sensorValue2);
    }

    return updatedDevice;
  }

  async logSensor(deviceId: string, metric: string, value: number) {
    return this.prisma.sensorLog.create({
      data: {
        deviceId,
        metric,
        value,
      },
    });
  }

  async getTelemetry(deviceId: string, metric: string, limit = 50) {
    return this.prisma.sensorLog.findMany({
      where: {
        deviceId,
        ...(metric && { metric }),
      },
      orderBy: { timestamp: 'desc' },
      take: limit,
    });
  }
}
