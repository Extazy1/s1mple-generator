import { listGeneratorVoByPageFastUsingPost } from '@/services/backend/generatorController';
import { UserOutlined } from '@ant-design/icons';
import {
  PageContainer,
  ProFormSelect,
  ProFormText,
  QueryFilter,
} from '@ant-design/pro-components';
import { Link } from '@umijs/max';
import {
  Avatar,
  Card,
  Flex,
  Image,
  Input,
  List,
  message,
  Tabs,
  Tag,
  Typography,
  theme,
} from 'antd';
import moment from 'moment';
import React, { useEffect, useState } from 'react';

/**
 * 默认分页参数
 */
const DEFAULT_PAGE_PARAMS: PageRequest = {
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
};

/**
 * 主页
 * @constructor
 */
const IndexPage: React.FC = () => {
  const { token } = theme.useToken(); // 取出主题 token，后面可用来自定义一些颜色等
  const [loading, setLoading] = useState<boolean>(true);
  const [dataList, setDataList] = useState<API.GeneratorVO[]>([]);
  const [total, setTotal] = useState<number>(0);
  // 搜索条件
  const [searchParams, setSearchParams] = useState<API.GeneratorQueryRequest>({
    ...DEFAULT_PAGE_PARAMS,
  });

  /**
   * 搜索
   */
  const doSearch = async () => {
    setLoading(true);
    try {
      const res = await listGeneratorVoByPageFastUsingPost(searchParams);
      setDataList(res.data?.records ?? []);
      setTotal(Number(res.data?.total) ?? 0);
    } catch (error: any) {
      message.error('获取数据失败，' + error.message);
    }
    setLoading(false);
  };

  useEffect(() => {
    doSearch();
  }, [searchParams]);

  /**
   * 标签列表
   * @param tags
   */
  const tagListView = (tags?: string[]) => {
    if (!tags?.length) {
      return <div style={{ color: 'red' }}>（无标签数据）</div>;
    }
    return (
      <div style={{ marginBottom: 8 }}>
        {tags.map((tag) => (
          <Tag key={tag} color="blue" style={{ marginBottom: 4 }}>
            {tag}
          </Tag>
        ))}
      </div>
    );
  };

  return (
    <PageContainer
      title={false}
      style={{
        backgroundColor: token.colorBgLayout,
        minHeight: '100vh',
      }}
    >
      {/* 顶部的标题与描述 */}
      <div
        style={{
          textAlign: 'center',
          padding: '32px 0 16px 0',
          background: 'linear-gradient(to right, #f8f9fa, #e9ecef)',
          borderRadius: 12,
          marginBottom: 24,
        }}
      >
        <Typography.Title
          level={2}
          style={{ marginBottom: 8, color: token.colorTextBase }}
        >
          欢迎来到代码生成器库
        </Typography.Title>
        <Typography.Text type="secondary">
          快速查找并使用你需要的代码生成器，提高工作效率
        </Typography.Text>

        {/* 搜索框居中 */}
        <Flex justify="center" style={{ marginBottom: 24 }}>
          <Input.Search
            style={{
              width: '40vw',
              minWidth: 320,
              borderRadius: 8,
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)',
            }}
            placeholder="搜索代码生成器"
            allowClear
            enterButton="搜索"
            size="large"
            onChange={(e) => {
              searchParams.searchText = e.target.value;
            }}
            onSearch={(value: string) => {
              setSearchParams({
                ...DEFAULT_PAGE_PARAMS,
                searchText: value,
              });
            }}
          />
        </Flex>
      </div>

      {/* 分类 Tab */}
      <Tabs
        size="large"
        defaultActiveKey="newest"
        items={[
          {
            key: 'newest',
            label: '最新',
          },
          {
            key: 'recommend',
            label: '推荐',
          },
        ]}
        onChange={(activeKey) => {
          // 根据 Tab 的 activeKey 改变搜索参数
          setSearchParams((prev) => ({
            ...prev,
            sortField: activeKey === 'newest' ? 'createTime' : 'name',
            sortOrder: activeKey === 'newest' ? 'descend' : 'descend',
          }));
        }}
        style={{
          marginBottom: 16,
        }}
      />

      {/* 筛选表单 */}
      <QueryFilter
        span={12}
        labelWidth="auto"
        labelAlign="left"
        defaultCollapsed={false}
        style={{
          padding: '16px',
          backgroundColor: token.colorBgContainer,
          borderRadius: 8,
          marginBottom: 24,
        }}
        onFinish={async (values: API.GeneratorQueryRequest) => {
          setSearchParams({
            ...DEFAULT_PAGE_PARAMS,
            // @ts-ignore
            ...values,
            searchText: searchParams.searchText,
          });
        }}
      >
        <ProFormSelect
          label="标签"
          name="tags"
          mode="tags"
          options={[
            { label: 'JavaScript', value: 'JavaScript' },
            { label: 'React', value: 'React' },
            { label: 'Node.js', value: 'Node.js' },
            { label: 'TypeScript', value: 'TypeScript' },
            { label: 'Java', value: 'Java' },
          ]}
        />
        <ProFormText label="名称" name="name" />
        <ProFormText label="描述" name="description" />
      </QueryFilter>

      {/* 列表展示 */}
      <List<API.GeneratorVO>
        rowKey="id"
        loading={loading}
        grid={{
          gutter: 16,
          xs: 1,
          sm: 2,
          md: 3,
          lg: 3,
          xl: 4,
          xxl: 4,
        }}
        dataSource={dataList}
        pagination={{
          current: searchParams.current,
          pageSize: searchParams.pageSize,
          total,
          onChange(current: number, pageSize: number) {
            setSearchParams({
              ...searchParams,
              current,
              pageSize,
            });
          },
        }}
        renderItem={(data) => (
          <List.Item>
            <Link to={`/generator/detail/${data.id}`}>
              <Card
                hoverable
                style={{
                  borderRadius: 8,
                  boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)',
                }}
                cover={
                  <Image
                    alt={data.name}
                    src={data.picture}
                    preview={false}
                    style={{
                      borderTopLeftRadius: 8,
                      borderTopRightRadius: 8,
                      objectFit: 'cover',
                      height: 180,
                    }}
                  />
                }
              >
                <Card.Meta
                  title={
                    <Typography.Title
                      level={5}
                      style={{ marginBottom: 8, color: token.colorTextBase }}
                    >
                      {data.name}
                    </Typography.Title>
                  }
                  description={
                    <Typography.Paragraph
                      ellipsis={{ rows: 2 }}
                      style={{ minHeight: 44, marginBottom: 0 }}
                    >
                      {data.description}
                    </Typography.Paragraph>
                  }
                />
                {tagListView(data.tags)}
                <Flex justify="space-between" align="center">
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {moment(data.createTime).fromNow()}
                  </Typography.Text>
                  <Avatar src={data.user?.userAvatar} icon={<UserOutlined />} />
                </Flex>
              </Card>
            </Link>
          </List.Item>
        )}
      />
    </PageContainer>
  );
};

export default IndexPage;
