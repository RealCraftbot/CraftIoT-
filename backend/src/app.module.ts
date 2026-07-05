import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { ThrottlerModule, ThrottlerGuard } from '@nestjs/throttler';
import { APP_GUARD } from '@nestjs/core';
import { PrismaModule } from './prisma/prisma.module';
import { AuthModule } from './auth/auth.module';
import { UsersModule } from './users/users.module';
import { DevicesModule } from './devices/devices.module';
import { MqttModule } from './mqtt/mqtt.module';
import { AutomationsModule } from './automations/automations.module';
import { NotificationsModule } from './notifications/notifications.module';
import { OtaModule } from './ota/ota.module';
import { AiModule } from './ai/ai.module';

@Module({
  imports: [
    // Global Configuration loader
    ConfigModule.forRoot({
      isGlobal: true,
    }),

    // System-wide API Rate Limiter
    ThrottlerModule.forRoot([{
      ttl: 60000, // 60 seconds
      limit: 100, // Maximum 100 requests per client per minute
    }]),

    PrismaModule,
    AuthModule,
    UsersModule,
    DevicesModule,
    MqttModule,
    AutomationsModule,
    NotificationsModule,
    OtaModule,
    AiModule,
  ],
  providers: [
    {
      provide: APP_GUARD,
      useClass: ThrottlerGuard,
    },
  ],
})
export class AppModule {}
