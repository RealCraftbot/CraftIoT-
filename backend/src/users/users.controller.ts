import { Controller, Get, Body, Patch, Param, Delete, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { UsersService } from './users.service';
import { UpdateUserDto } from './dto/update-user.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { Role } from '@prisma/client';
import { CurrentUser } from '../common/decorators/current-user.decorator';

@ApiTags('User Management')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard, RolesGuard)
@Controller('users')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Get()
  @Roles(Role.ADMIN)
  @ApiOperation({ summary: 'Retrieve all registered users (Admin Only)' })
  @ApiResponse({ status: 200, description: 'Return all users' })
  findAll() {
    return this.usersService.findAll();
  }

  @Get('me')
  @ApiOperation({ summary: 'Retrieve current profile' })
  getProfile(@CurrentUser() user: any) {
    return this.usersService.findOne(user.id);
  }

  @Get(':id')
  @Roles(Role.ADMIN)
  @ApiOperation({ summary: 'Retrieve specific user by ID (Admin Only)' })
  findOne(@Param('id') id: string) {
    return this.usersService.findOne(id);
  }

  @Patch(':id')
  @ApiOperation({ summary: 'Update user profile settings (Admin or Self)' })
  update(
    @Param('id') id: string,
    @Body() dto: UpdateUserDto,
    @CurrentUser() user: any,
  ) {
    return this.usersService.update(id, dto, user);
  }

  @Delete(':id')
  @ApiOperation({ summary: 'Delete user account (Admin or Self)' })
  remove(@Param('id') id: string, @CurrentUser() user: any) {
    return this.usersService.remove(id, user);
  }
}
