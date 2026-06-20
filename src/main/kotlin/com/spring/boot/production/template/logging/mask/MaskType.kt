package com.spring.boot.production.template.logging.mask

enum class MaskType {

    /**
     * Поле description
     * Любой текст -> **информация скрыта**
     */
    DESCRIPTION {
        override fun mask(value: String) = "Информация скрыта"
    },

    /**
     * Сплошная маскировка любого текста, кроме первого символа
     * Любой текст -> Л**** *****
     */
    FIRST_SYMBOL_ONLY {
        override fun mask(value: String): String = if (value.length <= 1) "*" else "*" + value.substring(1)
    },

    /**
     * Сплошная маскировка любого текста, кроме первых четверых символов
     * Любой текст -> *****gs1@gmail.com
     */
    FOUR_SYMBOL_ONLY {
        override fun mask(value: String): String = if (value.length <= 4) "****" else "****" + value.substring(4)
    },

    /**
     * Фамилия
     * Первая буква фамилии дополняется точкой, вне зависимости от дилнны фамилии:
     * Иванов -> И.
     */
    LAST_NAME {
        override fun mask(value: String): String = if (value.isEmpty()) "*" else value[0] + "."
    },

    /**
     * Сплошная маскировка любого номер мобильного телефона, кроме последних 2
     * Любой номер мобильного телефона -> 790321399**
     */
    PHONE {
        override fun mask(value: String): String = if (value.length <= 2) "**" else value.substring(0, value.length - 2) + "**"
    };

    abstract fun mask(value: String): String
}