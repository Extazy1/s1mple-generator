import React, { useEffect, useState } from 'react';
import { Link, useModel, useParams } from '@@/exports';
import { PageContainer } from '@ant-design/pro-components';
import {
  Button,
  Card,
  Col,
  Collapse,
  Form,
  Image,
  Input,
  message,
  Radio,
  Row,
  Select,
  Space,
  Tag,
  Typography,
} from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import { saveAs } from 'file-saver';
import {
  getGeneratorVoByIdUsingGet,
  useGeneratorUsingPost,
} from '@/services/backend/generatorController';

/**
 * 可根据后端返回的具体数据结构进行定义
 */
interface IModelField {
  fieldName: string;
  type?: string;
  description?: string;
  defaultValue?: any;
  groupKey?: string;
  groupName?: string;
  models?: IModelField[];
  options?: Array<{ label: string; value: any }>;
}

/**
 * 生成器使用
 * @constructor
 */
const GeneratorUsePage: React.FC = () => {
  const { id } = useParams();
  const [form] = Form.useForm();

  const [loading, setLoading] = useState<boolean>(false);
  const [downloading, setDownloading] = useState<boolean>(false);
  const [data, setData] = useState<API.GeneratorVO>({});
  const { initialState } = useModel('@@initialState');
  const { currentUser } = initialState ?? {};

  // 取出模型字段配置
  const models: IModelField[] = data?.modelConfig?.models ?? [];

  /**
   * 加载数据
   */
  const loadData = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const res = await getGeneratorVoByIdUsingGet({ id });
      setData(res.data || {});

      // 这里直接设置默认值
      if (res.data?.modelConfig?.models) {
        const defaultValues = getDefaultFormValues(res.data.modelConfig.models);
        form.setFieldsValue(defaultValues);
      }
    } catch (error: any) {
      message.error('获取数据失败，' + error.message);
    }
    setLoading(false);
  };

  /**
   * 递归解析字段默认值（含分组）
   */
  const getDefaultFormValues = (fields: IModelField[]) => {
    const values: Record<string, any> = {};
    fields.forEach((field) => {
      // 如果是分组
      if (field.groupKey && field.models?.length) {
        if (!values[field.groupKey]) {
          values[field.groupKey] = {};
        }
        field.models.forEach((subField) => {
          if (subField.defaultValue !== undefined) {
            // 若是布尔类型且默认值是字符串，可在此做进一步转换
            if (subField.type === 'boolean' && typeof subField.defaultValue === 'string') {
              values[field.groupKey][subField.fieldName] = subField.defaultValue === 'true';
            } else {
              values[field.groupKey][subField.fieldName] = subField.defaultValue;
            }
          }
        });
      } else {
        // 普通字段
        if (field.defaultValue !== undefined) {
          if (field.type === 'boolean' && typeof field.defaultValue === 'string') {
            values[field.fieldName] = field.defaultValue === 'true';
          } else {
            values[field.fieldName] = field.defaultValue;
          }
        }
      }
    });
    return values;
  };

  useEffect(() => {
    loadData();
  }, [id]);

  /**
   * 标签列表视图
   */
  const tagListView = (tags?: string[]) => {
    if (!tags || !tags.length) return null;
    return (
      <div style={{ marginBottom: 8 }}>
        {tags.map((tag: string) => (
          <Tag key={tag}>{tag}</Tag>
        ))}
      </div>
    );
  };

  /**
   * 根据字段类型渲染组件
   */
  const renderFormItem = (field: IModelField, groupKey?: string) => {
    const namePath = groupKey ? [groupKey, field.fieldName] : field.fieldName;

    switch (field.type) {
      case 'boolean':
        // 使用 Radio.Group + true/false
        return (
          <Form.Item
            label={field.fieldName}
            name={namePath}
            tooltip={field.description}
            key={field.fieldName}
            // 去掉 valuePropName="checked"，否则 Radio.Group 无法正常工作
          >
            <Radio.Group>
              <Radio value={true}>是</Radio>
              <Radio value={false}>否</Radio>
            </Radio.Group>
          </Form.Item>
        );
      case 'select':
        // 需要有可选项
        return (
          <Form.Item
            label={field.fieldName}
            name={namePath}
            tooltip={field.description}
            key={field.fieldName}
          >
            <Select placeholder={field.description}>
              {field.options?.map((opt) => (
                <Select.Option value={opt.value} key={opt.value}>
                  {opt.label}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
        );
      default:
        // 默认使用 Input
        return (
          <Form.Item
            label={field.fieldName}
            name={namePath}
            tooltip={field.description}
            key={field.fieldName}
          >
            <Input placeholder={field.description} />
          </Form.Item>
        );
    }
  };

  /**
   * 渲染下载按钮
   */
  const downloadButton = data.distPath && currentUser && (
    <Button
      type="primary"
      icon={<DownloadOutlined />}
      loading={downloading}
      onClick={async () => {
        setDownloading(true);
        try {
          const values = form.getFieldsValue();
          const blob = await useGeneratorUsingPost(
            {
              id: data.id,
              dataModel: values,
            },
            { responseType: 'blob' },
          );
          const fullPath = data.distPath || '';
          // 使用 file-saver 保存文件
          saveAs(blob, fullPath.substring(fullPath.lastIndexOf('/') + 1));
          message.success('下载成功！');
        } catch (error: any) {
          message.error('生成代码失败：' + error.message);
        } finally {
          setDownloading(false);
        }
      }}
    >
      生成代码
    </Button>
  );

  return (
    <PageContainer title={null} loading={loading}>
      <Card>
        <Row justify="space-between" gutter={[32, 32]}>
          <Col flex="auto">
            <Space size="large" align="center">
              <Typography.Title level={4}>{data.name}</Typography.Title>
              {tagListView(data.tags)}
            </Space>
            <Typography.Paragraph>{data.description}</Typography.Paragraph>
            <div style={{ marginBottom: 24 }} />

            <Form
              form={form}
              labelCol={{ span: 6 }}
              wrapperCol={{ span: 18 }}
              style={{ maxWidth: 800 }}
            >
              {/* 先渲染不带 groupKey 的字段 */}
              {models
                .filter((field) => !field.groupKey)
                .map((field) => {
                  return renderFormItem(field);
                })}

              {/* 再渲染带 groupKey 的分组字段 */}
              {models
                .filter((field) => field.groupKey && field.models?.length)
                .map((field, index) => {
                  return (
                    <Collapse
                      style={{ marginBottom: 24 }}
                      key={`collapse-${index}`}
                      defaultActiveKey={[index]}
                      bordered={false}
                      items={[
                        {
                          key: String(index),
                          label: `${field.groupName}（分组）`,
                          children: field.models!.map((subField) => {
                            return renderFormItem(subField, field.groupKey);
                          }),
                        },
                      ]}
                    />
                  );
                })}
            </Form>

            <Space size="middle">
              {downloadButton}
              <Link to={`/generator/detail/${id}`}>
                <Button>查看详情</Button>
              </Link>
            </Space>
          </Col>
          <Col flex="320px">
            <Image src={data.picture} />
          </Col>
        </Row>
      </Card>
    </PageContainer>
  );
};

export default GeneratorUsePage;
