import { Test, TestingModule } from '@nestjs/testing';
import { DevicesService } from './devices.service';
import { PrismaService } from '../prisma/prisma.service';
import { MqttService } from '../mqtt/mqtt.service';
import { ConflictException } from '@nestjs/common';

describe('DevicesService', () => {
  let service: DevicesService;
  let prisma: PrismaService;

  const mockPrismaService = {
    ioTDevice: {
      findUnique: jest.fn(),
      create: jest.fn(),
      findMany: jest.fn(),
      update: jest.fn(),
    },
    sensorLog: {
      create: jest.fn(),
    },
  };

  const mockMqttService = {
    subscribeToDevice: jest.fn(),
    publishToDevice: jest.fn(),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        DevicesService,
        { provide: PrismaService, useValue: mockPrismaService },
        { provide: MqttService, useValue: mockMqttService },
      ],
    }).compile();

    service = module.get<DevicesService>(DevicesService);
    prisma = module.get<PrismaService>(PrismaService);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('create', () => {
    it('should provision a new IoT device successfully', async () => {
      const dto = { id: 'dev01', name: 'Cool Device', type: 'ROBOT_CAR', macAddress: '00:11:22:33:44:55' };
      mockPrismaService.ioTDevice.findUnique.mockResolvedValueOnce(null); // ID unique
      mockPrismaService.ioTDevice.findUnique.mockResolvedValueOnce(null); // MAC unique
      mockPrismaService.ioTDevice.create.mockResolvedValue({ id: dto.id, ...dto });

      const result = await service.create(dto, 'user_uuid');

      expect(result).toBeDefined();
      expect(result.id).toBe(dto.id);
      expect(prisma.ioTDevice.create).toHaveBeenCalled();
      expect(mockMqttService.subscribeToDevice).toHaveBeenCalledWith(dto.id);
    });

    it('should throw ConflictException if device ID already registered', async () => {
      const dto = { id: 'existing', name: 'Cool Device', type: 'ROBOT_CAR', macAddress: '00:11:22:33:44:55' };
      mockPrismaService.ioTDevice.findUnique.mockResolvedValueOnce({ id: 'existing' });

      await expect(service.create(dto, 'user_uuid')).rejects.toThrow(ConflictException);
    });
  });
});
