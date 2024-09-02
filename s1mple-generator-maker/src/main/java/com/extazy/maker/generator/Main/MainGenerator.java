package com.extazy.maker.generator.Main;

import com.extazy.maker.meta.Meta;
import com.extazy.maker.meta.MetaManager;

public class MainGenerator {
    public static void main(String[] args) {
        Meta meta = MetaManager.getMetaObject();
        System.out.println(meta);
    }
}
