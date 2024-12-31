import ContextMenu from '@/components/ContentTool/ContextMenu';
import CreateModal from '@/pages/Admin/User/components/CreateModal';
import UpdateModal from '@/pages/Admin/User/components/UpdateModal';
import {deleteUserUsingPost, listUserByPageUsingPost} from '@/services/backend/userController';
import {PlusOutlined} from '@ant-design/icons';
import type {ActionType, ProColumns} from '@ant-design/pro-components';
import {PageContainer, ProTable} from '@ant-design/pro-components';
import '@umijs/max';
import {Button, message, Space, Typography} from 'antd';
import React, {useEffect, useRef, useState} from 'react';

/**
 * 用户管理页面
 *
 * @constructor
 */
const UserAdminPage: React.FC = () => {
  // 是否显示新建窗口
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  // 是否显示更新窗口
  const [updateModalVisible, setUpdateModalVisible] = useState<boolean>(false);
  // 当前用户点击的数据（双击或右键）
  const [currentRow, setCurrentRow] = useState<API.User>();

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
  const handleDelete = async (row: API.User) => {
    const hide = message.loading('正在删除');
    if (!row) return true;
    try {
      await deleteUserUsingPost({
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

  /**
   * 表格列配置
   */
  const columns: ProColumns<API.User>[] = [
    {
      title: 'id',
      dataIndex: 'id',
      valueType: 'text',
      hideInForm: true,
    },
    {
      title: '账号',
      dataIndex: 'userAccount',
      valueType: 'text',
    },
    {
      title: '用户名',
      dataIndex: 'userName',
      valueType: 'text',
    },
    {
      title: '头像',
      dataIndex: 'userAvatar',
      valueType: 'image',
      fieldProps: {
        width: 64,
      },
      hideInSearch: true,
    },
    {
      title: '简介',
      dataIndex: 'userProfile',
      valueType: 'textarea',
    },
    {
      title: '权限',
      dataIndex: 'userRole',
      valueEnum: {
        user: {
          text: '用户',
        },
        admin: {
          text: '管理员',
        },
        ban: {
          text: '封禁',
        },
      },
    },
    {
      title: '创建时间',
      sorter: true,
      dataIndex: 'createTime',
      valueType: 'dateTime',
      hideInSearch: true,
      hideInForm: true,
    },
    {
      title: '更新时间',
      sorter: true,
      dataIndex: 'updateTime',
      valueType: 'dateTime',
      hideInSearch: true,
      hideInForm: true,
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
    },
  ];
  return (
    <PageContainer
      title={false}
      header={{
        breadcrumb: {},
      }}
    >
      <Typography.Title level={4} style={{ marginBottom: 16 }}>
        用户管理
      </Typography.Title>
      <ProTable<API.User>
        headerTitle={false}
        actionRef={actionRef}
        rowKey="key"
        search={{
          labelWidth: 120,
        }}
        scroll={{ x: 'max-content' }}
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

          const { data, code } = await listUserByPageUsingPost({
            ...params,
            sortField,
            sortOrder,
            ...filter,
          } as API.UserQueryRequest);

          return {
            success: code === 0,
            data: data?.records || [],
            total: Number(data?.total) || 0,
          };
        }}
        columns={columns}
        onRow={(record) => {
          return {
            // 双击行时，进入修改页面
            onDoubleClick: () => {
              setCurrentRow(record);
              setUpdateModalVisible(true);
            },
            // 右键菜单逻辑
            onContextMenu: (event) => {
              event.preventDefault();
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
export default UserAdminPage;
