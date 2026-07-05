import { Module } from '@nestjs/common';
import { OtaService } from './ota.service';
import { OtaController } from './ota.controller';

@Module({
  controllers: [OtaController],
  providers: [OtaService],
  exports: [OtaService],
})
export class OtaModule {}
