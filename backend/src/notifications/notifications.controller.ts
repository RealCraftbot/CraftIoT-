import { Controller, Get, Delete, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { NotificationsService } from './notifications.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';

@ApiTags('Push Notification Inbox')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('notifications')
export class NotificationsController {
  constructor(private readonly notificationsService: NotificationsService) {}

  @Get()
  @ApiOperation({ summary: "Retrieve current user's historical notification inbox" })
  getUserNotifications(@CurrentUser() user: any) {
    return this.notificationsService.getUserNotifications(user.id);
  }

  @Delete()
  @ApiOperation({ summary: "Purge current user's notification inbox history" })
  clearUserNotifications(@CurrentUser() user: any) {
    return this.notificationsService.clearUserNotifications(user.id);
  }
}
