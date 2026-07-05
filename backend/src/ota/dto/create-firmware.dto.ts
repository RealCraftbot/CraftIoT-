import { IsString, IsNotEmpty, IsNumber } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class CreateFirmwareDto {
  @ApiProperty({ example: 'v1.1.0' })
  @IsString()
  @IsNotEmpty()
  version: string;

  @ApiProperty({ example: 'ROBOT_CAR', description: 'Device hardware profiles (ROBOT_CAR / CLIMATE_NODE)' })
  @IsString()
  @IsNotEmpty()
  deviceType: string;

  @ApiProperty({ example: 4.8 })
  @IsNumber()
  @IsNotEmpty()
  fileSizeMb: number;

  @ApiProperty({ example: 'https://storage.craftiot.com/firmware/robot_v1_1_0.bin' })
  @IsString()
  @IsNotEmpty()
  fileUrl: string;

  @ApiProperty({ example: 'Improved PWM motor synchronization and lower WiFi standby power draw' })
  @IsString()
  @IsNotEmpty()
  releaseNotes: string;

  @ApiProperty({ example: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855' })
  @IsString()
  @IsNotEmpty()
  checksum: string;
}
