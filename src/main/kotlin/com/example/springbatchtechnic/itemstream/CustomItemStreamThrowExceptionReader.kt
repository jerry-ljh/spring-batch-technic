package com.example.springbatchtechnic.itemstream

class CustomItemStreamThrowExceptionReader : CustomItemStreamReader() {

    override fun read(): Long? {
        val result = super.read()
        if (result == 8L) throw RuntimeException("read 8 error!")
        return result
    }
}