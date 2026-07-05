import { Injectable, Logger, OnModuleInit } from '@nestjs/common';
import Redis from 'ioredis';

@Injectable()
export class NotificationsService implements OnModuleInit {
  private redisClient: Redis;
  private readonly logger = new Logger('NotificationsService');
  private isRedisConnected = false;

  onModuleInit() {
    const host = process.env.REDIS_HOST || 'localhost';
    const port = Number(process.env.REDIS_PORT) || 6379;

    this.logger.log(`Connecting to Redis server at ${host}:${port}...`);
    this.redisClient = new Redis({
      host,
      port,
      lazyConnect: true,
      maxRetriesPerRequest: 1,
    });

    this.redisClient
      .connect()
      .then(() => {
        this.isRedisConnected = true;
        this.logger.log('Connected to Redis server successfully!');
      })
      .catch((err) => {
        this.logger.warn(`Redis connection failed (Continuing in in-memory fallback): ${err.message}`);
      });
  }

  async sendPushNotification(userId: string, title: string, body: string, data?: any) {
    this.logger.log(`[PUSH NOTIFICATION] Sending to User: ${userId} | Title: "${title}" | Body: "${body}"`);

    const notificationPayload = {
      id: Math.random().toString(36).slice(2, 11),
      userId,
      title,
      body,
      data: data || {},
      sentAt: new Date().toISOString(),
      read: false,
    };

    // Store in Redis cache for user notification history retrieval
    if (this.isRedisConnected) {
      try {
        const key = `user:${userId}:notifications`;
        await this.redisClient.lpush(key, JSON.stringify(notificationPayload));
        await this.redisClient.ltrim(key, 0, 99); // Keep latest 100 notifications
      } catch (err) {
        this.logger.error('Failed to cache notification in Redis', err.stack);
      }
    }

    return notificationPayload;
  }

  async getUserNotifications(userId: string) {
    if (this.isRedisConnected) {
      try {
        const key = `user:${userId}:notifications`;
        const cached = await this.redisClient.lrange(key, 0, -1);
        return cached.map((item) => JSON.parse(item));
      } catch (err) {
        this.logger.error('Failed to fetch notifications from Redis', err.stack);
      }
    }
    return []; // Return empty if redis fails or isn't connected
  }

  async clearUserNotifications(userId: string) {
    if (this.isRedisConnected) {
      try {
        const key = `user:${userId}:notifications`;
        await this.redisClient.del(key);
      } catch (err) {
        this.logger.error('Failed to clear notifications in Redis', err.stack);
      }
    }
    return { success: true };
  }
}
