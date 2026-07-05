import { Test, TestingModule } from '@nestjs/testing';
import { AuthService } from './auth.service';
import { PrismaService } from '../prisma/prisma.service';
import { JwtService } from '@nestjs/jwt';
import { ConflictException } from '@nestjs/common';
import { Role } from '@prisma/client';

describe('AuthService', () => {
  let service: AuthService;
  let prisma: PrismaService;

  const mockPrismaService = {
    user: {
      findUnique: jest.fn(),
      create: jest.fn(),
      count: jest.fn(),
      update: jest.fn(),
    },
  };

  const mockJwtService = {
    signAsync: jest.fn().mockResolvedValue('mocked_token'),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AuthService,
        { provide: PrismaService, useValue: mockPrismaService },
        { provide: JwtService, useValue: mockJwtService },
      ],
    }).compile();

    service = module.get<AuthService>(AuthService);
    prisma = module.get<PrismaService>(PrismaService);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('register', () => {
    it('should register a new user successfully', async () => {
      const dto = { email: 'test@example.com', password: 'password123', fullName: 'Tester McTest' };
      mockPrismaService.user.findUnique.mockResolvedValue(null);
      mockPrismaService.user.count.mockResolvedValue(1); // not first user
      mockPrismaService.user.create.mockResolvedValue({
        id: 'user_uuid',
        email: dto.email,
        fullName: dto.fullName,
        role: Role.USER,
        createdAt: new Date(),
        updatedAt: new Date(),
      });

      const result = await service.register(dto);

      expect(result).toBeDefined();
      expect(result.user.email).toBe(dto.email);
      expect(result.access_token).toBe('mocked_token');
      expect(prisma.user.create).toHaveBeenCalled();
    });

    it('should throw ConflictException if user already registered', async () => {
      const dto = { email: 'exists@example.com', password: 'password123', fullName: 'Exist' };
      mockPrismaService.user.findUnique.mockResolvedValue({ id: 'existing_id' });

      await expect(service.register(dto)).rejects.toThrow(ConflictException);
    });
  });
});
