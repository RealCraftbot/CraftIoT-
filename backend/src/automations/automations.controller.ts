import { Controller, Get, Post, Body, Param, Patch, Delete, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { AutomationsService } from './automations.service';
import { CreateRuleDto } from './dto/create-rule.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';

@ApiTags('Automation Engine')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('automations')
export class AutomationsController {
  constructor(private readonly automationsService: AutomationsService) {}

  @Post()
  @ApiOperation({ summary: 'Create a new real-time device automation rule' })
  @ApiResponse({ status: 201, description: 'Rule registered successfully' })
  create(@Body() createRuleDto: CreateRuleDto) {
    return this.automationsService.create(createRuleDto);
  }

  @Get()
  @ApiOperation({ summary: 'Retrieve all automation rules' })
  findAll() {
    return this.automationsService.findAll();
  }

  @Get('device/:deviceId')
  @ApiOperation({ summary: 'Retrieve rules specifically triggered by device' })
  findByDevice(@Param('deviceId') deviceId: string) {
    return this.automationsService.findByDevice(deviceId);
  }

  @Patch(':id/toggle')
  @ApiOperation({ summary: 'Activate or suspend an automation rule' })
  toggleRule(
    @Param('id') id: string,
    @Body('isActive') isActive: boolean,
  ) {
    return this.automationsService.toggleRule(id, isActive);
  }

  @Delete(':id')
  @ApiOperation({ summary: 'De-provision an automation rule' })
  deleteRule(@Param('id') id: string) {
    return this.automationsService.deleteRule(id);
  }
}
