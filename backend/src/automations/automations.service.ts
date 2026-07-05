import { Injectable, NotFoundException, Logger, Inject, forwardRef } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateRuleDto } from './dto/create-rule.dto';
import { MqttService } from '../mqtt/mqtt.service';
import { NotificationsService } from '../notifications/notifications.service';
import { IoTDevice } from '@prisma/client';

@Injectable()
export class AutomationsService {
  private readonly logger = new Logger('AutomationsService');

  constructor(
    private prisma: PrismaService,
    @Inject(forwardRef(() => MqttService))
    private mqttService: MqttService,
    private notificationsService: NotificationsService,
  ) {}

  async create(dto: CreateRuleDto) {
    // Validate trigger device
    const triggerDevice = await this.prisma.ioTDevice.findUnique({
      where: { id: dto.deviceId },
    });
    if (!triggerDevice) {
      throw new NotFoundException(`Trigger device with ID ${dto.deviceId} not found`);
    }

    // Validate action device
    const actionDevice = await this.prisma.ioTDevice.findUnique({
      where: { id: dto.actionDeviceId },
    });
    if (!actionDevice) {
      throw new NotFoundException(`Action target device with ID ${dto.actionDeviceId} not found`);
    }

    return this.prisma.automationRule.create({
      data: dto,
    });
  }

  async findAll() {
    return this.prisma.automationRule.findMany({
      include: {
        device: true,
        actionDevice: true,
      },
    });
  }

  async findByDevice(deviceId: string) {
    return this.prisma.automationRule.findMany({
      where: { deviceId },
    });
  }

  async toggleRule(id: string, isActive: boolean) {
    const rule = await this.prisma.automationRule.findUnique({ where: { id } });
    if (!rule) {
      throw new NotFoundException(`Automation rule with ID ${id} not found`);
    }

    return this.prisma.automationRule.update({
      where: { id },
      data: { isActive },
    });
  }

  async deleteRule(id: string) {
    const rule = await this.prisma.automationRule.findUnique({ where: { id } });
    if (!rule) {
      throw new NotFoundException(`Automation rule with ID ${id} not found`);
    }

    await this.prisma.automationRule.delete({ where: { id } });
    return { success: true, message: `Rule ${id} removed` };
  }

  async evaluateRulesForDevice(device: IoTDevice) {
    // Find all active rules triggered by this device ID
    const rules = await this.prisma.automationRule.findMany({
      where: {
        deviceId: device.id,
        isActive: true,
      },
    });

    for (const rule of rules) {
      let currentValue: number;
      if (rule.metric === 'sensorValue1') {
        currentValue = device.sensorValue1;
      } else if (rule.metric === 'sensorValue2') {
        currentValue = device.sensorValue2;
      } else {
        continue;
      }

      const isTriggered = this.evaluateCondition(currentValue, rule.operator, rule.thresholdValue);

      if (isTriggered) {
        this.logger.log(`Automation Rule [${rule.name}] TRIGGERED! (Value: ${currentValue} ${rule.operator} ${rule.thresholdValue})`);
        await this.executeAction(rule, device);
      }
    }
  }

  private evaluateCondition(currentValue: number, operator: string, threshold: number): boolean {
    switch (operator) {
      case 'GREATER_THAN':
        return currentValue > threshold;
      case 'LESS_THAN':
        return currentValue < threshold;
      case 'EQUALS':
        return currentValue === threshold;
      default:
        return false;
    }
  }

  private async executeAction(rule: any, triggerDevice: IoTDevice) {
    this.logger.log(`Executing Action for rule [${rule.name}] on actionDevice: ${rule.actionDeviceId}...`);

    try {
      // 1. Publish command over MQTT to target device
      const controlPayload = {
        deviceId: rule.actionDeviceId,
        stateFlag1: rule.actionType === 'TURN_ON' ? true : false,
        timestamp: new Date().toISOString(),
      };

      this.mqttService.publishToDevice(rule.actionDeviceId, 'control', controlPayload);

      // 2. Also update database value for action target device
      await this.prisma.ioTDevice.update({
        where: { id: rule.actionDeviceId },
        data: {
          stateFlag1: controlPayload.stateFlag1,
          lastActive: new Date(),
        },
      });

      // 3. Emit a real-time Push Notification alert to owners of the trigger device
      const deviceOwners = await this.prisma.deviceOwnership.findMany({
        where: { deviceId: triggerDevice.id },
        include: { user: true },
      });

      for (const owner of deviceOwners) {
        await this.notificationsService.sendPushNotification(
          owner.userId,
          'Automation Trigger Alert',
          `Rule "${rule.name}" triggered: ${rule.metric} exceeded threshold. ${rule.actionType} command dispatched to ${rule.actionDeviceId}.`,
          { ruleId: rule.id, deviceId: triggerDevice.id }
        );
      }
    } catch (err) {
      this.logger.error(`Error executing automation action: ${err.message}`);
    }
  }
}
