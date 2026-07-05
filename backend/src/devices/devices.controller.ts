import { Controller, Get, Post, Body, Param, Delete, UseGuards, Query } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth, ApiQuery } from '@nestjs/swagger';
import { DevicesService } from './devices.service';
import { CreateDeviceDto } from './dto/create-device.dto';
import { ShareDeviceDto } from './dto/share-device.dto';
import { ControlDeviceDto } from './dto/control-device.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';

@ApiTags('Device Management')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('devices')
export class DevicesController {
  constructor(private readonly devicesService: DevicesService) {}

  @Post()
  @ApiOperation({ summary: 'Register/Provision a new IoT device' })
  @ApiResponse({ status: 201, description: 'Device registered successfully' })
  @ApiResponse({ status: 409, description: 'Device ID or MAC Address conflict' })
  create(@Body() createDeviceDto: CreateDeviceDto, @CurrentUser() user: any) {
    return this.devicesService.create(createDeviceDto, user.id);
  }

  @Get()
  @ApiOperation({ summary: 'Retrieve all owned and shared devices' })
  findAll(@CurrentUser() user: any) {
    return this.devicesService.findAll(user);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Retrieve specific device metadata' })
  findOne(@Param('id') id: string, @CurrentUser() user: any) {
    return this.devicesService.findOne(id, user);
  }

  @Post(':id/share')
  @ApiOperation({ summary: 'Share device access with another user' })
  @ApiResponse({ status: 200, description: 'Device shared successfully' })
  share(
    @Param('id') id: string,
    @Body() shareDeviceDto: ShareDeviceDto,
    @CurrentUser() user: any,
  ) {
    return this.devicesService.share(id, shareDeviceDto, user);
  }

  @Delete(':id/share/:userId')
  @ApiOperation({ summary: 'Revoke shared device access' })
  unshare(
    @Param('id') id: string,
    @Param('userId') userId: string,
    @CurrentUser() user: any,
  ) {
    return this.devicesService.unshare(id, userId, user);
  }

  @Post(':id/control')
  @ApiOperation({ summary: 'Send command controls to activate device states' })
  control(
    @Param('id') id: string,
    @Body() controlDeviceDto: ControlDeviceDto,
    @CurrentUser() user: any,
  ) {
    return this.devicesService.control(id, controlDeviceDto, user);
  }

  @Get(':id/telemetry')
  @ApiOperation({ summary: 'Fetch recent historical sensor metrics (for visualization/charts)' })
  @ApiQuery({ name: 'metric', required: false, description: 'Filter by specific metric name' })
  @ApiQuery({ name: 'limit', required: false, type: Number, description: 'Number of logs to fetch' })
  getTelemetry(
    @Param('id') id: string,
    @Query('metric') metric?: string,
    @Query('limit') limit?: number,
  ) {
    return this.devicesService.getTelemetry(id, metric, limit ? Number(limit) : 50);
  }
}
