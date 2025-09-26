import { withLayout } from '@sb/decorators/withLayout';

import Skeleton from './Skeleton';

import type { Meta, StoryObj } from '@storybook/react-webpack5';

const meta = {
  component: Skeleton,
  decorators: [withLayout],
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof Skeleton>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
};
