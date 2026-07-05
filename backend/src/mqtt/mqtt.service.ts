import { Injectable, OnModuleInit, OnModuleDestroy, Logger, Inject, forwardRef } from '@nestjs/common';
import * as mqtt from 'mqtt';
import { PrismaService } from '../prisma/prisma.service';
import { DevicesService } from '../devices/devices.service';
import { AutomationsService } from '../automations/automations.service';

@Injectable()
export class MqttService implements OnModuleInit, OnModuleDestroy {
  private client: mqtt.MqttClient;
  private readonly logger = new Logger('MqttService');

  constructor(
    private prisma: PrismaService,
    @Inject(forwardRef(() => DevicesService))
    private devicesService: DevicesService,
    @Inject(forwardRef(() => AutomationsService))
    private automationsService: AutomationsService,
  ) {}

  onModuleInit() {
    const brokerUrl = process.env.MQTT_BROKER_URL || 'mqtt://broker.hivemq.com:1883';
    this.logger.log(`Connecting to MQTT broker at ${brokerUrl}...`);

    this.client = mqtt.connect(brokerUrl, {
      clientId: `craftiot_backend_${Math.random().toString(16).slice(2, 10)}`,
      clean: true,
      reconnectPeriod: 5000,
    });

    this.client.on('connect', () => {
      this.logger.log('Connected to MQTT Broker successfully!');
      // Subscribe to all device telemetry streams
      this.client.subscribe('craftiot/devices/+/telemetry', (err) => {
        if (!err) {
          this.logger.log('Subscribed to all device telemetry streams: craftiot/devices/+/telemetry');
        } else {
          this.logger.error('Failed to subscribe to telemetry streams', err.stack);
        }
      });
    });

    this.client.on('message', async (topic, message) => {
      await this.handleIncomingMessage(topic, message.toString());
    });

    this.client.on('error', (error) => {
      this.logger.error('MQTT Connection error:', error.stack);
    });
  }

  onModuleDestroy() {
    if (this.client) {
      this.client.end();
    }
  }

  subscribeToDevice(deviceId: string) {
    if (this.client && this.client.connected) {
      const topic = `craftiot/devices/${deviceId}/telemetry`;
      this.client.subscribe(topic, (err) => {
        if (!err) {
          this.logger.log(`Subscribed specifically to device topic: ${topic}`);
        }
      });
    }
  }

  publishToDevice(deviceId: string, action: string, payload: any) {
    if (this.client && this.client.connected) {
      const topic = `craftiot/devices/${deviceId}/${action}`;
      this.client.publish(topic, JSON.stringify(payload), { qos: 1 }, (err) => {
        if (err) {
          this.logger.error(`Failed to publish control message to ${topic}`, err.stack);
        } else {
          this.logger.log(`Published control command to ${topic}: ${JSON.stringify(payload)}`);
        }
      });
    } else {
      this.logger.warn(`MQTT client not connected. Drop publish to device ${deviceId}`);
    }
  }

  private async handleIncomingMessage(topic: string, messageStr: string) {
    this.logger.log(`Received MQTT message [${topic}]: ${messageStr}`);

    try {
      // Matches craftiot/devices/{deviceId}/telemetry
      const regex = /^craftiot\/devices\/([^/]+)\/telemetry$/;
      const match = topic.match(regex);

      if (match) {
        const deviceId = match[1];
        const payload = JSON.parse(messageStr);

        const { sensorValue1, sensorValue2, stateFlag1 } = payload;

        // 1. Update latest device status in Database
        const device = await this.prisma.ioTDevice.update({
          where: { id: deviceId },
          data: {
            ...(sensorValue1 !== undefined && { sensorValue1 }),
            ...(sensorValue2 !== undefined && { sensorValue2 }),
            ...(stateFlag1 !== undefined && { stateFlag1 }),
            lastActive: new Date(),
          },
        });

        // 2. Log historical entries in database
        if (sensorValue1 !== undefined) {
          await this.devicesService.logSensor(deviceId, 'sensorValue1', sensorValue1);
        }
        if (sensorValue2 !== undefined) {
          await this.devicesService.logSensor(deviceId, 'sensorValue2', sensorValue2);
        }

        // 3. Trigger Real-Time Automation rules evaluation
        await this.automationsService.evaluateRulesForDevice(device);
      }
    } catch (e) {
      this.logger.error(`Error processing incoming MQTT message on topic ${topic}: ${e.message}`);
    }
  }
}
