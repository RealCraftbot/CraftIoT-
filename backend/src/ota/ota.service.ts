import { Injectable, NotFoundException, ConflictException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateFirmwareDto } from './dto/create-firmware.dto';

@Injectable()
export class OtaService {
  constructor(private prisma: PrismaService) {}

  async create(dto: CreateFirmwareDto) {
    const existing = await this.prisma.firmwareRelease.findFirst({
      where: {
        version: dto.version,
        deviceType: dto.deviceType,
      },
    });

    if (existing) {
      throw new ConflictException(`Firmware version ${dto.version} already exists for ${dto.deviceType}`);
    }

    return this.prisma.firmwareRelease.create({
      data: dto,
    });
  }

  async findAll() {
    return this.prisma.firmwareRelease.findMany({
      orderBy: { createdAt: 'desc' },
    });
  }

  async findLatest(deviceType: string) {
    const latest = await this.prisma.firmwareRelease.findFirst({
      where: { deviceType },
      orderBy: { version: 'desc' }, // simple alphabetical version sorting, can use semver in full production
    });

    if (!latest) {
      throw new NotFoundException(`No firmware releases found for device type: ${deviceType}`);
    }

    return latest;
  }

  async checkUpdate(deviceType: string, currentVersion: string) {
    try {
      const latest = await this.findLatest(deviceType);
      
      // Basic check: version equality. 
      const updateAvailable = latest.version !== currentVersion;
      
      return {
        updateAvailable,
        latestVersion: latest.version,
        fileUrl: updateAvailable ? latest.fileUrl : null,
        checksum: updateAvailable ? latest.checksum : null,
        fileSizeMb: updateAvailable ? latest.fileSizeMb : null,
        releaseNotes: updateAvailable ? latest.releaseNotes : null,
      };
    } catch {
      return {
        updateAvailable: false,
        message: `No releases cataloged for ${deviceType}`,
      };
    }
  }
}
