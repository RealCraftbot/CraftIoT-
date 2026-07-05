import { Module, forwardRef } from '@nestjs/common';
import { AutomationsService } from './automations.service';
import { AutomationsController } from './automations.controller';
import { MqttModule } from '../mqtt/mqtt.module';
import { NotificationsModule } from '../notifications/notifications.module';

@Module({
  imports: [
    forwardRef(() => MqttModule),
    NotificationsModule,
  ],
  controllers: [AutomationsController],
  providers: [AutomationsService],
  exports: [AutomationsService],
})
export class AutomationsModule {}
