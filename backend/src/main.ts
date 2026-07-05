import { NestFactory } from '@nestjs/core';
import { ValidationPipe, Logger } from '@nestjs/common';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import { AppModule } from './app.module';
import { HttpExceptionFilter } from './common/filters/http-exception.filter';
import { LoggingInterceptor } from './common/interceptors/logging.interceptor';

async function bootstrap() {
  const logger = new Logger('Bootstrap');
  const app = await NestFactory.create(AppModule);

  // Enable Cross-Origin Resource Sharing (CORS) for Android/Web clients
  app.enableCors({
    origin: '*',
    methods: 'GET,HEAD,PUT,PATCH,POST,DELETE,OPTIONS',
    credentials: true,
  });

  // Global Input Validation checks
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      transform: true,
      forbidNonWhitelisted: true,
    }),
  );

  // Global HTTP error interceptor
  app.useGlobalFilters(new HttpExceptionFilter());

  // Global performance request-response logger
  app.useGlobalInterceptors(new LoggingInterceptor());

  // Swagger OpenAPI Spec configuration
  const config = new DocumentBuilder()
    .setTitle('CraftIoT Platform APIs')
    .setDescription(
      'Production-ready backend API documentation for the CraftIoT platform, supporting smart climate telemetry, autonomous triggers, and OTA releases.',
    )
    .setVersion('1.0.0')
    .addBearerAuth()
    .build();

  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api/docs', app, document);

  const port = process.env.PORT || 3000;
  await app.listen(port);
  
  logger.log(`========================================================`);
  logger.log(`  CraftIoT Backend Server listening on port: ${port}`);
  logger.log(`  Interactive API Docs available at: http://localhost:${port}/api/docs`);
  logger.log(`========================================================`);
}
bootstrap();
