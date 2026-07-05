import { Module, forwardRef } from '@nestjs/common';
import { MqttService } from './mqtt.service';
import { DevicesModule } from '../devices/devices.module';
import { AutomationsModule } from '../automations/automations.module';

@Module({
  imports: [
    forwardRef(() => DevicesModule),
    forwardRef(() => AutomationsModule),
  ],
  providers: [MqttService],
  exports: [MqttService],
})
export class MqttModule {}
