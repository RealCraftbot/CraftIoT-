import { Injectable, ConflictException, UnauthorizedException, BadRequestException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { PrismaService } from '../prisma/prisma.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
import * as bcrypt from 'bcrypt';
import { Role } from '@prisma/client';

@Injectable()
export class AuthService {
  constructor(
    private prisma: PrismaService,
    private jwtService: JwtService,
  ) {}

  async hashData(data: string): Promise<string> {
    return bcrypt.hash(data, 10);
  }

  async getTokens(userId: string, email: string, role: Role) {
    const [accessToken, refreshToken] = await Promise.all([
      this.jwtService.signAsync(
        { sub: userId, email, role },
        {
          secret: process.env.JWT_ACCESS_SECRET || 'craftiot_access_jwt_super_secret_key_2026',
          expiresIn: '15m',
        },
      ),
      this.jwtService.signAsync(
        { sub: userId, email, role },
        {
          secret: process.env.JWT_REFRESH_SECRET || 'craftiot_refresh_jwt_super_secret_key_2026',
          expiresIn: '7d',
        },
      ),
    ]);

    return {
      access_token: accessToken,
      refresh_token: refreshToken,
    };
  }

  async register(dto: RegisterDto) {
    const existingUser = await this.prisma.user.findUnique({
      where: { email: dto.email },
    });
    if (existingUser) {
      throw new ConflictException('Email address already registered');
    }

    const passwordHash = await this.hashData(dto.password);
    
    // First user is automatically ADMIN for easy bootstrapping
    const userCount = await this.prisma.user.count();
    const role = userCount === 0 ? Role.ADMIN : Role.USER;

    const newUser = await this.prisma.user.create({
      data: {
        email: dto.email,
        passwordHash,
        fullName: dto.fullName,
        role,
      },
    });

    const tokens = await this.getTokens(newUser.id, newUser.email, newUser.role);
    await this.updateRefreshToken(newUser.id, tokens.refresh_token);
    return { user: this.sanitizeUser(newUser), ...tokens };
  }

  async login(dto: LoginDto) {
    const user = await this.prisma.user.findUnique({
      where: { email: dto.email },
    });
    if (!user || !user.passwordHash) {
      throw new UnauthorizedException('Invalid login credentials');
    }

    const passwordMatches = await bcrypt.compare(dto.password, user.passwordHash);
    if (!passwordMatches) {
      throw new UnauthorizedException('Invalid login credentials');
    }

    const tokens = await this.getTokens(user.id, user.email, user.role);
    await this.updateRefreshToken(user.id, tokens.refresh_token);
    return { user: this.sanitizeUser(user), ...tokens };
  }

  async logout(userId: string) {
    await this.prisma.user.update({
      where: { id: userId },
      data: { refreshTokenHash: null },
    });
    return { success: true };
  }

  async refreshTokens(userId: string, rt: string) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
    });
    if (!user || !user.refreshTokenHash) {
      throw new UnauthorizedException('Access Denied');
    }

    const rtMatches = await bcrypt.compare(rt, user.refreshTokenHash);
    if (!rtMatches) {
      throw new UnauthorizedException('Access Denied');
    }

    const tokens = await this.getTokens(user.id, user.email, user.role);
    await this.updateRefreshToken(user.id, tokens.refresh_token);
    return tokens;
  }

  async googleLogin(googleUser: any) {
    if (!googleUser) {
      throw new BadRequestException('Google Authentication profile is missing');
    }

    let user = await this.prisma.user.findUnique({
      where: { email: googleUser.email },
    });

    if (!user) {
      user = await this.prisma.user.create({
        data: {
          email: googleUser.email,
          fullName: googleUser.fullName,
          googleId: googleUser.googleId,
          role: Role.USER,
        },
      });
    } else if (!user.googleId) {
      // Bind Google account to existing email registration
      user = await this.prisma.user.update({
        where: { id: user.id },
        data: { googleId: googleUser.googleId },
      });
    }

    const tokens = await this.getTokens(user.id, user.email, user.role);
    await this.updateRefreshToken(user.id, tokens.refresh_token);
    return { user: this.sanitizeUser(user), ...tokens };
  }

  async updateRefreshToken(userId: string, refreshToken: string) {
    const hash = await this.hashData(refreshToken);
    await this.prisma.user.update({
      where: { id: userId },
      data: { refreshTokenHash: hash },
    });
  }

  sanitizeUser(user: any) {
    const { passwordHash, refreshTokenHash, ...sanitized } = user;
    return sanitized;
  }
}
