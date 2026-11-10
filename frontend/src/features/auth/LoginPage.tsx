import { useState } from 'react'
import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { loginAdmin } from './authApi'

const { Title, Text } = Typography

interface LoginPageProps { onLoggedIn: () => Promise<void> }

export default function LoginPage({ onLoggedIn }: LoginPageProps) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true)
    setError(null)
    try {
      await loginAdmin(values.username.trim(), values.password)
      await onLoggedIn()
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setLoading(false)
    }
  }
  return (
    <div className="pottel-login-page">
      <Card className="pottel-login-card">
        <Title level={3} style={{ marginTop: 0 }}>Pottel Reporting System</Title>
        <Text type="secondary">Đăng nhập bằng tài khoản admin hiện có của Pottel BSS.</Text>
        {error && <Alert style={{ marginTop: 18 }} showIcon type="error" message="Không thể đăng nhập" description={error} />}
        <Form layout="vertical" onFinish={onFinish} autoComplete="on" style={{ marginTop: 24 }}>
          <Form.Item label="Tên đăng nhập" name="username" rules={[{ required: true, message: 'Nhập tên admin' }]}>
            <Input size="large" prefix={<UserOutlined />} autoComplete="username" maxLength={50} />
          </Form.Item>
          <Form.Item label="Mật khẩu" name="password" rules={[{ required: true, message: 'Nhập mật khẩu' }]}>
            <Input.Password size="large" prefix={<LockOutlined />} autoComplete="current-password" />
          </Form.Item>
          <Button block size="large" type="primary" htmlType="submit" loading={loading}>Đăng nhập</Button>
        </Form>
        <Text type="secondary" style={{ fontSize: 12 }}>Chỉ tài khoản có vai trò admin và chưa bị khóa mới được truy cập.</Text>
      </Card>
    </div>
  )
}
