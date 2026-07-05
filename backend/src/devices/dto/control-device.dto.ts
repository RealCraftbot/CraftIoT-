import { IsBoolean, IsNumber, IsOptional } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class ControlDeviceDto {
  @ApiProperty({ example: true, required: false })
  @IsBoolean()
  @IsOptional()
  stateFlag1?: boolean;

  @ApiProperty({ example: 45.5, required: false })
  @IsNumber()
  @IsOptional()
  sensorValue1?: number;

  @ApiProperty({ example: 22.0, required: false })
  @IsNumber()
  @IsOptional()
  sensorValue2?: number;
}
