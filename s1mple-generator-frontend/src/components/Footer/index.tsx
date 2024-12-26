import { GithubOutlined } from '@ant-design/icons';
import { DefaultFooter } from '@ant-design/pro-components';
import '@umijs/max';
import React from 'react';

const Footer: React.FC = () => {
  const defaultMessage = 'Extazy';
  const currentYear = new Date().getFullYear();
  return (
    <DefaultFooter
      style={{
        background: 'none',
      }}
      copyright={`${currentYear} ${defaultMessage}`}
      links={[
        {
          key: 'React',
          title: 'React',
          href: 'https://zh-hans.react.dev/learn',
          blankTarget: true,
        },
        {
          key: 'AntDesign',
          title: 'AntDesign',
          href: 'https://ant.design/index-cn',
          blankTarget: true,
        },
        {
          key: 'github',
          title: (
            <>
              <GithubOutlined /> 项目源码
            </>
          ),
          href: 'https://github.com/Extazy1/s1mple-generator',
          blankTarget: true,
        },
      ]}
    />
  );
};
export default Footer;
