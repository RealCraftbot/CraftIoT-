import { IsString, IsNotEmpty } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class AskAiDto {
  @ApiProperty({ example: 'Can you summarize my home energy consumption and state flags?', description: 'Natural language question' })
  @IsString()
  @IsNotEmpty()
  prompt: string;
}
