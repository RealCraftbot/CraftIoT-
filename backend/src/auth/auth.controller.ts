import { Controller, Post, Body, UseGuards, HttpCode, HttpStatus, Get, Req, Res } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { AuthService } from './auth.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
import { JwtAuthGuard } from './guards/jwt-auth.guard';
import { JwtRefreshGuard } from './guards/jwt-refresh.guard';
import { GoogleOauthGuard } from './guards/google-oauth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';

@ApiTags('Authentication')
@Controller('auth')
export class AuthController {
  constructor(private authService: AuthService) {}

  @Post('register')
  @ApiOperation({ summary: 'Register a new user' })
  @ApiResponse({ status: 201, description: 'User created successfully' })
  @ApiResponse({ status: 409, description: 'Conflict - User already exists' })
  async register(@Body() dto: RegisterDto) {
    return this.authService.register(dto);
  }

  @Post('login')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Login with email and password' })
  @ApiResponse({ status: 200, description: 'Login successful, returns JWT credentials' })
  @ApiResponse({ status: 401, description: 'Unauthorized' })
  async login(@Body() dto: LoginDto) {
    return this.authService.login(dto);
  }

  @UseGuards(JwtAuthGuard)
  @Post('logout')
  @ApiBearerAuth()
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Logout and terminate active refresh tokens' })
  async logout(@CurrentUser() user: any) {
    return this.authService.logout(user.id);
  }

  @UseGuards(JwtRefreshGuard)
  @Post('refresh')
  @ApiBearerAuth()
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Rotate and fetch new Access Token pairs' })
  async refresh(@CurrentUser() user: any) {
    return this.authService.refreshTokens(user.id, user.refreshToken);
  }

  @Get('google')
  @UseGuards(GoogleOauthGuard)
  @ApiOperation({ summary: 'Initiate Google OAuth2 Single Sign-On flow' })
  async googleAuth() {}

  @Get('google/redirect')
  @UseGuards(GoogleOauthGuard)
  @ApiOperation({ summary: 'Internal Google callback redirect handler' })
  async googleAuthRedirect(@Req() req: any) {
    return this.authService.googleLogin(req.user);
  }
}
