import { IsEmail, IsEnum, IsNotEmpty } from 'class-validator';
import { SharePermission } from '@prisma/client';
import { ApiProperty } from '@nestjs/swagger';

export class ShareDeviceDto {
  @ApiProperty({ example: 'colleague@example.com', description: 'Email of the user to share the device with' })
  @IsEmail()
  @IsNotEmpty()
  email: string;

  @ApiProperty({ example: 'READ', enum: SharePermission })
  @IsEnum(SharePermission)
  @IsNotEmpty()
  permission: SharePermission;
}
