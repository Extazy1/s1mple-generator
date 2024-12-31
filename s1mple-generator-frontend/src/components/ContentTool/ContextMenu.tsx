import React from 'react';

interface ContextMenuProps {
  visible: boolean;
  position: { x: number; y: number };
  onUpdate: () => void;
  onDelete: () => void;
  onClose: () => void;
}

const ContextMenu: React.FC<ContextMenuProps> = ({
  visible,
  position,
  onUpdate,
  onDelete,
  onClose,
}) => {
  if (!visible) {
    return null;
  }

  return (
    <div
      style={{
        position: 'fixed',
        top: position.y - window.scrollY,
        left: position.x - window.scrollX,
        backgroundColor: '#fff',
        borderRadius: '8px',
        border: '1px solid #d9d9d9',
        padding: '8px',
        zIndex: 9999,
        boxShadow: '0 4px 16px rgba(0, 0, 0, 0.1)',
        overflow: 'hidden', // 防止子元素溢出
        fontFamily: 'Arial, sans-serif',
        minWidth: '80px',
      }}
      onClick={(e) => {
        e.stopPropagation(); // 防止触发其他点击事件
        onClose(); // 点击菜单关闭
      }}
    >
      <div
        style={{
          padding: '8px 12px',
          cursor: 'pointer',
          color: '#333',
          transition: 'background-color 0.2s ease',
        }}
        onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#f5f5f5')} // 鼠标悬停时颜色变化
        onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
        onClick={(e) => {
          e.stopPropagation(); // 阻止冒泡
          onUpdate();
        }}
      >
        修改
      </div>
      <div
        style={{
          padding: '8px 12px',
          cursor: 'pointer',
          color: '#ff4d4f',
          transition: 'background-color 0.2s ease',
        }}
        onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#fff1f0')} // 鼠标悬停时颜色变化
        onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
        onClick={(e) => {
          e.stopPropagation(); // 阻止冒泡
          onDelete();
        }}
      >
        删除
      </div>
    </div>
  );
};

export default ContextMenu;
