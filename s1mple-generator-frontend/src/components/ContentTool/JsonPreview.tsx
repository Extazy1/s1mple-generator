import { Modal } from 'antd';
import React, { useState } from 'react';

interface JsonPreviewProps {
  jsonString?: string;
  maxLength?: number; // 表格中预览时显示的最大字符数
  title?: string; // Modal 标题
}

/**
 * 用于在表格中简短预览 JSON，并可点击查看完整 JSON 的组件
 */
const JsonPreview: React.FC<JsonPreviewProps> = (props) => {
  const { jsonString = '', maxLength = 20, title = '详情查看' } = props;
  const [modalVisible, setModalVisible] = useState(false);

  // 尝试解析 JSON
  let parsed: any;
  try {
    parsed = JSON.parse(jsonString);
  } catch {
    // 非 JSON 字符串就直接显示
    parsed = null;
  }

  // 截断以在表格中显示片段
  const shortText =
    jsonString.length > maxLength ? `${jsonString.substring(0, maxLength)} ...` : jsonString;

  return (
    <>
      {/* 简短显示区域 */}
      <div
        style={{
          padding: 6,
          borderRadius: 4,
          maxWidth: 300,
          minHeight: 32,
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          cursor: 'pointer',
        }}
        onClick={() => setModalVisible(true)}
      >
        {shortText}
      </div>

      {/* 弹窗显示完整内容 */}
      <Modal
        width={600}
        title={title}
        visible={modalVisible}
        footer={null}
        onCancel={() => setModalVisible(false)}
      >
        <div style={{ backgroundColor: '#fafafa', padding: 12, borderRadius: 4 }}>
          <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordWrap: 'break-word' }}>
            {parsed ? JSON.stringify(parsed, null, 2) : jsonString}
          </pre>
        </div>
      </Modal>
    </>
  );
};

export default JsonPreview;
