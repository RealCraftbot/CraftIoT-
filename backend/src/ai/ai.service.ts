import { Injectable, Logger, InternalServerErrorException } from '@nestjs/common';
import { GoogleGenerativeAI } from '@google/generative-ai';
import { PrismaService } from '../prisma/prisma.service';
import { DevicesService } from '../devices/devices.service';

@Injectable()
export class AiService {
  private readonly logger = new Logger('AiService');
  private ai: GoogleGenerativeAI | null = null;

  constructor(
    private prisma: PrismaService,
    private devicesService: DevicesService,
  ) {
    const apiKey = process.env.GEMINI_API_KEY;
    if (apiKey && apiKey !== 'your_gemini_api_key_from_secrets_panel') {
      this.ai = new GoogleGenerativeAI(apiKey);
      this.logger.log('Gemini API initialized successfully!');
    } else {
      this.logger.warn('Gemini API Key missing or unconfigured. AI features will fallback gracefully.');
    }
  }

  async askAssistant(userId: string, prompt: string, userProfile: any) {
    this.logger.log(`Processing AI request for user ${userId}: "${prompt}"`);

    // 1. Fetch user's devices & latest state configurations
    const devicesData = await this.devicesService.findAll(userProfile);
    
    // 2. Fetch user's automation rules
    const rules = await this.prisma.automationRule.findMany({
      where: {
        device: {
          owners: { some: { userId } }
        }
      },
      include: {
        device: { select: { name: true } },
        actionDevice: { select: { name: true } }
      }
    });

    // 3. Assemble contextual prompt
    const systemContext = `
You are the CraftIoT intelligent voice and text assistant. You are speaking with User: ${userProfile.fullName} (Email: ${userProfile.email}).
Here is the user's current physical IoT inventory and active configurations:

DEVICES OWNED:
${JSON.stringify(devicesData, null, 2)}

AUTOMATION RULES ACTIVE:
${JSON.stringify(rules.map(r => ({
  ruleName: r.name,
  triggerDevice: r.device.name,
  metric: r.metric,
  triggerOperator: r.operator,
  thresholdValue: r.thresholdValue,
  actionDevice: r.actionDevice.name,
  actionType: r.actionType,
  isActive: r.isActive
})), null, 2)}

Provide a highly professional, conversational, helpful response to the user's prompt. 
Be concise. Keep instructions simple. Answer using clear layout and advice.
    `;

    if (!this.ai) {
      // Fallback response for offline sandbox development or missing credentials
      return {
        response: `[SIMULATED ASSISTANT] Hello ${userProfile.fullName}! (Note: Gemini API key is not set). Based on your current device profile: You have ${rules.length} active automations. If your devices report any issues, please check your network connection or provide a valid GEMINI_API_KEY inside your .env configuration.`,
        simulated: true,
      };
    }

    try {
      const model = this.ai.getGenerativeModel({ model: 'gemini-2.5-flash' });
      const result = await model.generateContent({
        contents: [
          { role: 'user', parts: [{ text: systemContext + `\n\nUser Question: ${prompt}` }] }
        ]
      });

      return {
        response: result.response.text(),
        simulated: false,
      };
    } catch (err) {
      this.logger.error('Failed to generate content with Gemini', err.stack);
      throw new InternalServerErrorException(`AI engine error: ${err.message}`);
    }
  }
}
