import { IsString, IsOptional, IsEnum } from 'class-validator';
import { Role } from '@prisma/client';
import { ApiProperty } from '@nestjs/swagger';

export class UpdateUserDto {
  @ApiProperty({ example: 'John Doe', required: false })
  @IsString()
  @IsOptional()
  fullName?: string;

  @ApiProperty({ example: 'TECHNICIAN', enum: Role, required: false })
  @IsEnum(Role)
  @IsOptional()
  role?: Role;
}
