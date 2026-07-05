import { Controller, Post, Body, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { AiService } from './ai.service';
import { AskAiDto } from './dto/ask-ai.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';

@ApiTags('AI Assistant')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('ai')
export class AiController {
  constructor(private readonly aiService: AiService) {}

  @Post('ask')
  @ApiOperation({ summary: 'Consult the CraftIoT AI assistant with standard natural language' })
  @ApiResponse({ status: 200, description: 'Return structured AI analytical assessment' })
  ask(@Body() dto: AskAiDto, @CurrentUser() user: any) {
    return this.aiService.askAssistant(user.id, dto.prompt, user);
  }
}
