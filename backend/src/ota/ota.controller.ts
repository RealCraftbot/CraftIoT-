import { Controller, Get, Post, Body, UseGuards, Query, Param } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth, ApiQuery } from '@nestjs/swagger';
import { OtaService } from './ota.service';
import { CreateFirmwareDto } from './dto/create-firmware.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { Role } from '@prisma/client';

@ApiTags('OTA Firmware Management')
@Controller('ota')
export class OtaController {
  constructor(private readonly otaService: OtaService) {}

  @Post()
  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(Role.ADMIN, Role.TECHNICIAN)
  @ApiOperation({ summary: 'Publish and register a new OTA firmware package (Admin/Technician Only)' })
  @ApiResponse({ status: 201, description: 'Firmware registered successfully' })
  create(@Body() createFirmwareDto: CreateFirmwareDto) {
    return this.otaService.create(createFirmwareDto);
  }

  @Get()
  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(Role.ADMIN, Role.TECHNICIAN)
  @ApiOperation({ summary: 'List all published firmware packages (Admin/Technician Only)' })
  findAll() {
    return this.otaService.findAll();
  }

  @Get('latest/:deviceType')
  @ApiOperation({ summary: 'Retrieve the latest firmware release for a hardware profile' })
  findLatest(@Param('deviceType') deviceType: string) {
    return this.otaService.findLatest(deviceType);
  }

  @Get('check-update')
  @ApiOperation({ summary: 'Devices query this endpoint to verify update eligibility' })
  @ApiQuery({ name: 'deviceType', example: 'ROBOT_CAR' })
  @ApiQuery({ name: 'currentVersion', example: 'v1.0.0' })
  checkUpdate(
    @Query('deviceType') deviceType: string,
    @Query('currentVersion') currentVersion: string,
  ) {
    return this.otaService.checkUpdate(deviceType, currentVersion);
  }
}
