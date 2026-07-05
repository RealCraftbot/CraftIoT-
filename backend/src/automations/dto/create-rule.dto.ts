import { IsString, IsNotEmpty, IsNumber, IsBoolean, IsOptional } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class CreateRuleDto {
  @ApiProperty({ example: 'Auto Cool Living Room' })
  @IsString()
  @IsNotEmpty()
  name: string;

  @ApiProperty({ example: 'esp32_climate_01', description: 'Trigger source device ID' })
  @IsString()
  @IsNotEmpty()
  deviceId: string;

  @ApiProperty({ example: 'sensorValue1', description: 'Metric key to monitor (sensorValue1 / sensorValue2)' })
  @IsString()
  @IsNotEmpty()
  metric: string;

  @ApiProperty({ example: 'GREATER_THAN', description: 'Operator: GREATER_THAN, LESS_THAN, EQUALS' })
  @IsString()
  @IsNotEmpty()
  operator: string;

  @ApiProperty({ example: 30.0 })
  @IsNumber()
  @IsNotEmpty()
  thresholdValue: number;

  @ApiProperty({ example: 'esp32_relay_02', description: 'Action target device ID' })
  @IsString()
  @IsNotEmpty()
  actionDeviceId: string;

  @ApiProperty({ example: 'TURN_ON', description: 'Action to trigger (TURN_ON, TURN_OFF, ALERT)' })
  @IsString()
  @IsNotEmpty()
  actionType: string;
}
