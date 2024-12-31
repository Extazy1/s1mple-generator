import ContextMenu from '@/components/ContentTool/ContextMenu';
import JsonPreview from '@/components/ContentTool/JsonPreview';
import CreateModal from '@/pages/Admin/Generator/components/CreateModal';
import UpdateModal from '@/pages/Admin/Generator/components/UpdateModal';
import {deleteGeneratorUsingPost, listGeneratorByPageUsingPost,} from '@/services/backend/generatorController';
import {FolderOutlined, PlusOutlined} from '@ant-design/icons';
import type {ActionType, ProColumns} from '@ant-design/pro-components';
import {PageContainer, ProTable} from '@ant-design/pro-components';
import '@umijs/max';
import {Button, message, Select, Space, Tag, Tooltip, Typography} from 'antd';
import React, {useEffect, useRef, useState} from 'react';

/**
 * 生成器管理页面
 *
 * @constructor
 */
const GeneratorAdminPage: React.FC = () => {
  // 是否显示新建窗口
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  // 是否显示更新窗口
  const [updateModalVisible, setUpdateModalVisible] = useState<boolean>(false);
  // 当前用户点击的数据
  const [currentRow, setCurrentRow] = useState<API.Generator>();
  // 右键菜单相关状态
  const [contextMenuVisible, setContextMenuVisible] = useState<boolean>(false);
  const [contextMenuPosition, setContextMenuPosition] = useState<{
    x: number;
    y: number;
  }>({ x: 0, y: 0 });
  const [contextMenuRow, setContextMenuRow] = useState<API.User>();

  // ProTable 的操作引用,用于在修改表格后进行刷新
  const actionRef = useRef<ActionType>();

  /**
   * 删除节点
   *
   * @param row
   */
  const handleDelete = async (row: API.Generator) => {
    const hide = message.loading('正在删除');
    if (!row) return true;
    try {
      await deleteGeneratorUsingPost({
        id: row.id as any,
      });
      hide();
      message.success('删除成功');
      actionRef?.current?.reload();
      return true;
    } catch (error: any) {
      hide();
      message.error('删除失败，' + error.message);
      return false;
    }
  };

  /**
   * 表格列配置
   */
  const columns: ProColumns<API.Generator>[] = [
    {
      title: 'id',
      dataIndex: 'id',
      valueType: 'text',
      hideInForm: true,
      align: 'center',
    },
    {
      title: '名称',
      dataIndex: 'name',
      valueType: 'text',
      align: 'center',
    },
    {
      title: '描述',
      dataIndex: 'description',
      valueType: 'textarea',
      align: 'left',
    },
    {
      title: '基础包',
      dataIndex: 'basePackage',
      valueType: 'text',
      hideInSearch: true,
      align: 'center',
    },
    {
      title: '版本',
      dataIndex: 'version',
      valueType: 'text',
      hideInSearch: true,
      align: 'center',
    },
    {
      title: '作者',
      dataIndex: 'author',
      valueType: 'text',
      align: 'center',
    },
    {
      title: '标签',
      dataIndex: 'tags',
      valueType: 'text',
      renderFormItem: (schema) => {
        const { fieldProps } = schema;
        // @ts-ignore
        return <Select {...fieldProps} mode="tags" />;
      },
      render(_, record) {
        if (!record.tags) {
          return <></>;
        }
        return JSON.parse(record.tags).map((tag: string) => {
          return (
            <Tag key={tag} color="blue" style={{ marginRight: 4 }}>
              {tag}
            </Tag>
          );
        });
      },
      align: 'center',
    },
    {
      title: '文件配置',
      dataIndex: 'fileConfig',
      hideInSearch: true,
      render: (_, record) => {
        const { fileConfig } = record;
        return fileConfig ? (
          <JsonPreview jsonString={fileConfig} maxLength={60} title="文件配置详情" />
        ) : (
          <Typography.Text type="secondary">无</Typography.Text>
        );
      },
      align: 'center',
    },
    {
      title: '模型配置',
      dataIndex: 'modelConfig',
      hideInSearch: true,
      render: (_, record) => {
        const { modelConfig } = record;
        return modelConfig ? (
          <JsonPreview jsonString={modelConfig} maxLength={60} title="模型配置详情" />
        ) : (
          <Typography.Text type="secondary">无</Typography.Text>
        );
      },
      align: 'center',
    },
    {
      title: '输出路径',
      dataIndex: 'distPath',
      hideInSearch: true,
      render: (_, record) => {
        const { distPath } = record;
        if (!distPath) {
          return <Typography.Text type="secondary">无</Typography.Text>;
        }
        return (
          <Tooltip title={distPath}>
            <Typography.Link
              style={{
                maxWidth: 150,
                display: 'inline-block',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
              href={distPath.startsWith('http') ? distPath : undefined}
              target="_blank"
            >
              <FolderOutlined style={{ marginRight: 4 }} />
              {distPath}
            </Typography.Link>
          </Tooltip>
        );
      },
      align: 'center',
    },
    {
      title: '图片',
      dataIndex: 'picture',
      valueType: 'image',
      fieldProps: {
        width: 64,
        style: { borderRadius: 4, objectFit: 'cover', margin: '0 auto' },
      },
      hideInSearch: true,
      align: 'center',
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueEnum: {
        0: {
          text: '默认',
          status: 'Default',
        },
        // 1: {
        //   text: '未知',
        //   status: 'Undefined',
        // },
      },
      align: 'center',
    },
    {
      title: '创建用户',
      dataIndex: 'userId',
      valueType: 'text',
      hideInForm: true,
      align: 'center',
    },
    {
      title: '创建时间',
      sorter: true,
      dataIndex: 'createTime',
      valueType: 'dateTime',
      hideInSearch: true,
      hideInForm: true,
      align: 'center',
    },
    {
      title: '更新时间',
      sorter: true,
      dataIndex: 'updateTime',
      valueType: 'dateTime',
      hideInSearch: true,
      hideInForm: true,
      align: 'center',
    },
    {
      title: '操作',
      dataIndex: 'option',
      valueType: 'option',
      render: (_, record) => (
        <Space size="middle">
          <Typography.Link
            onClick={() => {
              setCurrentRow(record);
              setUpdateModalVisible(true);
            }}
          >
            修改
          </Typography.Link>
          <Typography.Link type="danger" onClick={() => handleDelete(record)}>
            删除
          </Typography.Link>
        </Space>
      ),
      align: 'center',
    },
  ];

  // 点击其他区域隐藏右键菜单
  useEffect(() => {
    // 关闭菜单的事件处理
    const handleClick = () => {
      if (contextMenuVisible) {
        setContextMenuVisible(false);
      }
    };

    // 监听滚动事件关闭菜单
    const handleScroll = () => {
      if (contextMenuVisible) {
        setContextMenuVisible(false);
      }
    };

    window.addEventListener('click', handleClick); // 点击其他区域关闭菜单
    window.addEventListener('scroll', handleScroll); // 滚动时关闭菜单

    return () => {
      // 移除事件监听器
      window.removeEventListener('click', handleClick);
      window.removeEventListener('scroll', handleScroll);
    };
  }, [contextMenuVisible]);

  const handleUpdate = () => {
    if (contextMenuRow) {
      setCurrentRow(contextMenuRow);
    }
    setUpdateModalVisible(true);
    setContextMenuVisible(false);
  };

  const handleDeleteFromContextMenu = () => {
    if (contextMenuRow) {
      handleDelete(contextMenuRow);
    }
    setContextMenuVisible(false);
  };

  return (
    <PageContainer
      title={false}
      header={{
        breadcrumb: {},
      }}
      style={{
        maxWidth: 2400, // 设置最大宽度
        margin: '0 auto', // 居中对齐
      }}
    >
      <Typography.Title level={4} style={{ marginBottom: 16 }}>
        生成器管理
      </Typography.Title>
      <ProTable<API.Generator>
        headerTitle={false}
        actionRef={actionRef}
        rowKey="key"
        scroll={{ x: 'max-content' }}
        search={{
          labelWidth: 120,
        }}
        toolBarRender={() => [
          <Button
            type="primary"
            key="primary"
            onClick={() => {
              setCreateModalVisible(true);
            }}
          >
            <PlusOutlined /> 新建
          </Button>,
        ]}
        request={async (params, sort, filter) => {
          const sortField = Object.keys(sort)?.[0];
          const sortOrder = sort?.[sortField] ?? undefined;

          const { data, code } = await listGeneratorByPageUsingPost({
            ...params,
            sortField,
            sortOrder,
            ...filter,
          } as API.GeneratorQueryRequest);

          return {
            success: code === 0,
            data: data?.records || [],
            total: Number(data?.total) || 0,
          };
        }}
        columns={columns}
        onRow={(record) => {
          return {
            // 双击行时，进入修改页面，同时隐藏右键菜单
            onDoubleClick: (event) => {
              event.stopPropagation(); // 阻止事件传播
              setContextMenuVisible(false); // 确保右键菜单隐藏
              setCurrentRow(record);
              setUpdateModalVisible(true);
            },
            // 右键菜单逻辑
            onContextMenu: (event) => {
              event.preventDefault(); // 阻止浏览器默认右键行为
              if (event.detail === 2) {
                // 如果是双击，不执行右键逻辑
                return;
              }
              setContextMenuRow(record);
              setContextMenuPosition({ x: event.pageX, y: event.pageY });
              setContextMenuVisible(true);
            },
          };
        }}
      />

      {/* 右键菜单 - 仅在contextMenuVisible为true时显示 */}
      <ContextMenu
        visible={contextMenuVisible}
        position={contextMenuPosition}
        onUpdate={handleUpdate}
        onDelete={handleDeleteFromContextMenu}
        onClose={() => setContextMenuVisible(false)}
      />

      <CreateModal
        visible={createModalVisible}
        columns={columns}
        onSubmit={() => {
          setCreateModalVisible(false);
          actionRef.current?.reload();
        }}
        onCancel={() => {
          setCreateModalVisible(false);
        }}
      />
      <UpdateModal
        visible={updateModalVisible}
        columns={columns}
        oldData={currentRow}
        onSubmit={() => {
          setUpdateModalVisible(false);
          setCurrentRow(undefined);
          actionRef.current?.reload();
        }}
        onCancel={() => {
          setUpdateModalVisible(false);
        }}
      />
    </PageContainer>
  );
};
export default GeneratorAdminPage;
