import { FileOutlined, InfoCircleOutlined } from '@ant-design/icons';
import { Card, Collapse, Descriptions, DescriptionsProps, Space, Tag } from 'antd';
import React from 'react';

interface Props {
  data: API.GeneratorVO;
}

/**
 * 文件配置
 * @constructor
 */
const FileConfig: React.FC<Props> = (props) => {
  const { data } = props;
  const fileConfig = data?.fileConfig;

  if (!fileConfig) {
    return null;
  }

  /**
   * 渲染文件或分组列表
   */
  const renderFileList = (files?: API.FileInfo[]) => {
    if (!files || files.length === 0) {
      return <p style={{ color: '#999' }}>暂无文件配置</p>;
    }

    return (
      <Collapse bordered={false} style={{ backgroundColor: '#fff' }} accordion>
        {files.map((file, index) => {
          // 如果是分组
          if (file.groupKey) {
            return (
              <Collapse.Panel
                key={index}
                header={
                  <Space>
                    <Tag color="blue">{file.groupKey}</Tag>
                    {file.groupName}
                  </Space>
                }
              >
                <Descriptions column={1} size="small" bordered>
                  <Descriptions.Item label="分组 key">{file.groupKey}</Descriptions.Item>
                  <Descriptions.Item label="分组名">{file.groupName}</Descriptions.Item>
                  <Descriptions.Item label="条件">{file.condition}</Descriptions.Item>
                  <Descriptions.Item label="组内文件">
                    {renderFileList(file.files)}
                  </Descriptions.Item>
                </Descriptions>
              </Collapse.Panel>
            );
          }

          // 如果是单个文件
          return (
            <Collapse.Panel
              key={index}
              header={
                <Space>
                  <FileOutlined />
                  {file.inputPath || '未命名文件'}
                </Space>
              }
            >
              <Descriptions column={1} size="small" bordered>
                <Descriptions.Item label="输入路径">{file.inputPath}</Descriptions.Item>
                <Descriptions.Item label="输出路径">{file.outputPath}</Descriptions.Item>
                <Descriptions.Item label="文件类别">{file.type}</Descriptions.Item>
                <Descriptions.Item label="文件生成类别">{file.generateType}</Descriptions.Item>
                <Descriptions.Item label="条件">{file.condition}</Descriptions.Item>
              </Descriptions>
            </Collapse.Panel>
          );
        })}
      </Collapse>
    );
  };

  const basicInfoItems: DescriptionsProps['items'] = [
    {
      key: 'inputRootPath',
      label: '输入根路径',
      children: fileConfig.inputRootPath || '-',
    },
    {
      key: 'outputRootPath',
      label: '输出根路径',
      children: fileConfig.outputRootPath || '-',
    },
    {
      key: 'sourceRootPath',
      label: '项目根路径',
      children: fileConfig.sourceRootPath || '-',
    },
    {
      key: 'type',
      label: '文件类别',
      children: fileConfig.type || '-',
    },
  ];

  return (
    <div>
      <Card
        title={
          <Space>
            <InfoCircleOutlined />
            <span>基本信息</span>
          </Space>
        }
        style={{ marginBottom: 24 }}
      >
        <Descriptions bordered size="small" column={1} items={basicInfoItems} />
      </Card>

      <Card
        title={
          <Space>
            <FileOutlined />
            <span>文件列表</span>
          </Space>
        }
      >
        {renderFileList(fileConfig.files)}
      </Card>
    </div>
  );
};

export default FileConfig;
