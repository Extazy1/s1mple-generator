import { CloseOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Checkbox, Form, FormListFieldData, Input, Select, Space, message } from 'antd';
import { parseTemplateUsingPost } from '@/services/backend/generatorController';
import FileUploader from "@/components/FileUploader";
import { useState} from 'react';

interface Props {
  formRef: any;
  oldData: any;
}

export default (props: Props) => {
  const { formRef, oldData } = props;
  const [selectedKeys, setSelectedKeys] = useState<number[]>([]);

  // 上传文件并自动解析
  const handleUploadAndParse = async (file: any) => {
    try {
      // 1. 拿到后端返回的 fileUrl（不同的 FileUploader 可能字段名不一样）
      const fileUrl = file?.response;
      if (!fileUrl) {
        message.error('上传结果异常，未获取到文件地址');
        return;
      }
      // 2. 调用后端的 parseTemplate 接口
      const res = await parseTemplateUsingPost({ fileUrl });
      if (res.code !== 0) {
        message.error(res.message || '解析文件配置失败');
        return;
      }
      // 3. 后端返回了一个 Generator，其 fileConfig 是 JSON 字符串
      const generatorData = res.data || {};
      if (!generatorData.fileConfig) {
        message.error('后端未返回 fileConfig');
        return;
      }
      // 4. 将 JSON 字符串 parse 成对象
      const fileConfigObj = JSON.parse(generatorData.fileConfig);

      // 5. 用 formRef 把解析到的 fileConfig 写入表单
      //    fileConfigObj 里应该有 files: [ ... ] 结构
      formRef.current?.setFieldsValue({
        fileConfig: fileConfigObj, // 这里对应 antd Form.List name={['fileConfig','files']}
      });

      message.success('自动识别文件配置成功！');

      // 如果还想在下一步（生成器制作）里用到这个 fileUrl，可以存到 form 或上层 state：
      formRef.current?.setFieldsValue({
        fileConfig: fileConfigObj,
        templateUrl: fileUrl, // 赋值
      });
    } catch (err: any) {
      message.error('解析异常：' + err.message);
    }
  };

  /**
   * 处理复选框变更
   */
  const handleCheckboxChange = (idx: number, checked: boolean) => {
    setSelectedKeys((prev) => {
      if (checked) {
        // 如果勾选，就加入
        return [...prev, idx];
      } else {
        // 如果取消，就移除
        return prev.filter((k) => k !== idx);
      }
    });
  };

  /**
   * 合并分组：把 selectedKeys 对应的单文件合并成一个 group
   */
  const doCombineGroup = () => {
    // 从表单获取当前 fileConfig.files
    const currentValues = formRef.current?.getFieldsValue();
    const files = currentValues?.fileConfig?.files || [];

    if (!selectedKeys.length) {
      message.warning('请先勾选要合并的文件');
      return;
    }

    // 收集被勾选的单文件
    const selectedItems: any[] = [];
    // 未勾选的仍然保留
    const unselectedItems: any[] = [];
    // 按照 index 分拣
    files.forEach((item: any, idx: number) => {
      // 如果该条是“未分组文件”并且在 selectedKeys 里，才归入 selectedItems
      if (!item.groupKey && selectedKeys.includes(idx)) {
        selectedItems.push(item);
      } else {
        unselectedItems.push(item);
      }
    });

    if (!selectedItems.length) {
      message.warning('所选条目为空或已是分组');
      return;
    }

    // 构造一个新的分组对象
    const newGroup = {
      groupName: '自定义分组',
      groupKey: 'customGroup',
      type: 'group',
      // 把勾选的单文件塞到 group.files
      files: selectedItems,
    };

    // 在 unselectedItems 后面追加这个分组
    const newList = [...unselectedItems, newGroup];

    // 更新表单
    formRef.current?.setFieldsValue({
      fileConfig: {
        ...currentValues.fileConfig,
        files: newList,
      },
    });

    // 清空选中
    setSelectedKeys([]);
    message.success('合并分组成功');
  };

  /**
   * 单个文件表单视图
   * @param field
   * @param remove
   * @param isGroupFile 是否为组内文件
   */
  const singleFieldFormView = (
    field: FormListFieldData,
    index: number,
    remove?: (index: number | number[]) => void,
    isGroupFile: boolean = false, // 默认不是组内文件
  ) => {
    return (
      <Space align="center" key={field.key} style={{ display: 'flex', flexWrap: 'wrap', columnGap: 16 }}>
        {/* 只有在不是组内文件时才渲染 Checkbox */}
        {!isGroupFile && (
          <Checkbox
            checked={selectedKeys.includes(index)}
            onChange={(e) => handleCheckboxChange(index, e.target.checked)}
            style={{ marginRight: 8 }}
          />
        )}
        <Form.Item label="输入路径" name={[field.name, 'inputPath']} style={{ flex: 1 }}>
          <Input />
        </Form.Item>
        <Form.Item label="输出路径" name={[field.name, 'outputPath']} style={{ flex: 1 }}>
          <Input />
        </Form.Item>
        <Form.Item label="类型" name={[field.name, 'type']} style={{ flex: 1 }}>
          <Select
            style={{
              minWidth: 80,
            }}
            options={[
              { value: 'file', label: '文件' },
              { value: 'dir', label: '目录' },
            ]}
          />
        </Form.Item>
        <Form.Item label="生成类型" name={[field.name, 'generateType']} style={{ flex: 1 }}>
          <Select
            style={{
              minWidth: 80,
            }}
            options={[
              { value: 'static', label: '静态' },
              { value: 'dynamic', label: '动态' },
            ]}
          />
        </Form.Item>
        <Form.Item label="条件" name={[field.name, 'condition']} style={{ flex: 1 }}>
          <Input />
        </Form.Item>
        {remove && (
          <Button type="text" danger onClick={() => remove(field.name)} style={{ alignSelf: 'center' }}>
            删除
          </Button>
        )}
      </Space>
    );
  };

  return (
    <>
      <Form.Item name="templateUrl" hidden>
        <Input />
      </Form.Item>

      {/* 上传控件 */}
      <Form.Item label="上传模板压缩包（自动识别文件）">
        <FileUploader
          biz="generator_make_template"
          onChange={(fileList) => {
            // 取最后一个文件
            const file = fileList?.[fileList.length - 1];
            if (!file) return;

            // 当 status === 'done' 时，说明上传成功
            if (file.status === 'done') {
              // file.response 就是后端传回来的数据
              // 然后去调用 handleUploadAndParse(file)
              handleUploadAndParse(file);
            }
          }}
        />
      </Form.Item>

      <Alert message="如果不需要使用在线制作功能，可不填写" type="warning" closable />
      <div style={{ marginBottom: 16 }} />

      <Form.List name={['fileConfig', 'files']}>
        {(fields, { add, remove }) => {
          return (
            <div style={{ display: 'flex', rowGap: 16, flexDirection: 'column' }}>
              {fields.map((field, index) => {
                // 这里拿 index
                const currentValues = formRef?.current?.getFieldsValue()?.fileConfig ?? oldData?.fileConfig;
                const groupKey = currentValues.files?.[index]?.groupKey;

                if (groupKey) {
                  return (
                    <Card
                      size="small"
                      title="分组"
                      key={field.key}
                      extra={<CloseOutlined onClick={() => remove(field.name)} />}
                    >
                      {/* 分组视图 */}
                      <Space>
                        <Form.Item label="分组key" name={[field.name, 'groupKey']}>
                          <Input />
                        </Form.Item>
                        <Form.Item label="组名" name={[field.name, 'groupName']}>
                          <Input />
                        </Form.Item>
                        <Form.Item label="条件" name={[field.name, 'condition']}>
                          <Input />
                        </Form.Item>
                      </Space>

                      <Form.Item label="组内文件">
                        <Form.List name={[field.name, 'files']}>
                          {(subFields, subOpt) => (
                            <div style={{ display: 'flex', flexDirection: 'column', rowGap: 16 }}>
                              {subFields.map((subField) =>
                                singleFieldFormView(subField, subField.name, subOpt.remove, true),
                              )}
                              <Button type="dashed" onClick={() => subOpt.add()} block>
                                添加组内文件
                              </Button>
                            </div>
                          )}
                        </Form.List>
                      </Form.Item>
                    </Card>
                  );
                }

                // 否则就是单文件视图
                return (
                  <Card
                    size="small"
                    title="未分组文件"
                    key={field.key}
                    extra={<CloseOutlined onClick={() => remove(field.name)} />}
                  >
                    {singleFieldFormView(field, index, remove)}
                  </Card>
                );
              })}

              <Button type="dashed" onClick={() => add()}>
                添加文件
              </Button>
              <Button
                type="dashed"
                onClick={() =>
                  add({
                    groupName: '分组',
                    groupKey: 'group',
                    type: 'group',
                  })
                }
              >
                添加分组
              </Button>

              {/* 合并分组按钮 */}
              <Button type="dashed" onClick={doCombineGroup}>
                合并分组
              </Button>

              <div style={{ marginBottom: 16 }} />
            </div>
          );
        }}
      </Form.List>
    </>
  );
};
