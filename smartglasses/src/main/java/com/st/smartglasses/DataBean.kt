package com.st.smartglasses

data class DataBean(
        val imageRes: Int,
        val viewType: Int
) {
    companion object {
        fun getTestData(): List<DataBean> {
            return listOf(
                    DataBean(R.drawable.image1, 1),
                    DataBean(R.drawable.image2, 1),
                    DataBean(R.drawable.image3, 1)
            )
        }
    }
}
