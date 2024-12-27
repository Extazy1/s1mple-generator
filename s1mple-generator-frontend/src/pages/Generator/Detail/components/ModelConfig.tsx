import React from 'react';
import { Descriptions, Card, Collapse, Space, Tag } from 'antd';
import { FileOutlined } from '@ant-design/icons';

interface Props {
  data: API.GeneratorVO;
}

/**
 * 模型配置
 * @constructor
 */
const ModelConfig: React.FC<Props> = (props) => {
  const { data } = props;
  const modelConfig = data?.modelConfig;

  if (!modelConfig) {
    return null;
  }

  const renderModelList = (models?: API.ModelInfo[]) => {
    if (!models || models.length === 0) {
      return <p style={{ color: '#999' }}>暂无模型配置</p>;
    }

    return (
      <Collapse
        bordered={false}
        style={{ backgroundColor: '#fff' }}
        accordion
      >
        {models.map((model, index) => {
          // 如果是分组
          if (model.groupKey) {
            return (
              <Collapse.Panel
                key={index}
                header={
                  <Space>
                    <Tag color="purple">{model.groupKey}</Tag>
                    {model.groupName}
                  </Space>
                }
              >
                <Descriptions column={1} size="small" bordered>
                  <Descriptions.Item label="分组 key">{model.groupKey}</Descriptions.Item>
                  <Descriptions.Item label="分组名">{model.groupName}</Descriptions.Item>
                  <Descriptions.Item label="条件">{model.condition}</Descriptions.Item>
                  <Descriptions.Item label="组内模型">
                    {renderModelList(model.models)}
                  </Descriptions.Item>
                </Descriptions>
              </Collapse.Panel>
            );
          }

          // 如果是单个模型
          return (
            <Collapse.Panel
              key={index}
              header={model.fieldName || '未命名模型字段'}
            >
              <Descriptions column={1} size="small" bordered>
                <Descriptions.Item label="字段名称">{model.fieldName}</Descriptions.Item>
                <Descriptions.Item label="类型">{model.type}</Descriptions.Item>
                <Descriptions.Item label="描述">{model.description}</Descriptions.Item>
                <Descriptions.Item label="默认值">
                  {model.defaultValue as any}
                </Descriptions.Item>
                <Descriptions.Item label="缩写">{model.abbr}</Descriptions.Item>
                <Descriptions.Item label="条件">{model.condition}</Descriptions.Item>
              </Descriptions>
            </Collapse.Panel>
          );
        })}
      </Collapse>
    );
  };

  return (
    <div>
      <Card
        title={
          <Space>
            <FileOutlined />
            <span>模型列表</span>
          </Space>
        }
      >
        {renderModelList(modelConfig.models)}
      </Card>
    </div>
  );
};

export default ModelConfig;
