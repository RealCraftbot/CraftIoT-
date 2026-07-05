import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { UpdateUserDto } from './dto/update-user.dto';
import { Role } from '@prisma/client';

@Injectable()
export class UsersService {
  constructor(private prisma: PrismaService) {}

  async findAll() {
    const users = await this.prisma.user.findMany({
      orderBy: { createdAt: 'desc' },
    });
    return users.map((user) => this.sanitizeUser(user));
  }

  async findOne(id: string) {
    const user = await this.prisma.user.findUnique({
      where: { id },
    });
    if (!user) {
      throw new NotFoundException(`User with ID ${id} not found`);
    }
    return this.sanitizeUser(user);
  }

  async update(id: string, dto: UpdateUserDto, currentUser: any) {
    // If updating role, ensure target exists and operator is ADMIN
    if (dto.role && currentUser.role !== Role.ADMIN) {
      throw new ForbiddenException('Only administrators can promote/demote system roles');
    }

    const user = await this.prisma.user.findUnique({ where: { id } });
    if (!user) {
      throw new NotFoundException(`User with ID ${id} not found`);
    }

    const updated = await this.prisma.user.update({
      where: { id },
      data: dto,
    });

    return this.sanitizeUser(updated);
  }

  async remove(id: string, currentUser: any) {
    if (currentUser.role !== Role.ADMIN && currentUser.id !== id) {
      throw new ForbiddenException('You do not have permission to delete this account');
    }

    const user = await this.prisma.user.findUnique({ where: { id } });
    if (!user) {
      throw new NotFoundException(`User with ID ${id} not found`);
    }

    await this.prisma.user.delete({ where: { id } });
    return { success: true, message: `User account ${id} successfully unregistered` };
  }

  private sanitizeUser(user: any) {
    const { passwordHash, refreshTokenHash, ...sanitized } = user;
    return sanitized;
  }
}
