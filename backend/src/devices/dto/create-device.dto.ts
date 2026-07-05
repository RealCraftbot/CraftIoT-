import { IsString, IsNotEmpty, IsOptional, IsEnum } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class CreateDeviceDto {
  @ApiProperty({ example: 'esp32_climate_01', description: 'Unique device identifier (Serial / MAC)' })
  @IsString()
  @IsNotEmpty()
  id: string;

  @ApiProperty({ example: 'Living Room Temp Sensor' })
  @IsString()
  @IsNotEmpty()
  name: string;

  @ApiProperty({ example: 'CLIMATE_NODE', description: 'Type of device (ROBOT_CAR, CLIMATE_NODE, SMART_AGRI, SECURITY_CAM)' })
  @IsString()
  @IsNotEmpty()
  type: string;

  @ApiProperty({ example: '192.168.1.100', required: false })
  @IsString()
  @IsOptional()
  ipAddress?: string;

  @ApiProperty({ example: 'AA:BB:CC:DD:EE:FF' })
  @IsString()
  @IsNotEmpty()
  macAddress: string;

  @ApiProperty({ example: 'WIFI', default: 'WIFI', required: false })
  @IsString()
  @IsOptional()
  connectionType?: string;
}
