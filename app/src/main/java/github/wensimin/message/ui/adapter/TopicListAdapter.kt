package github.wensimin.message.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import github.wensimin.message.MainActivity
import github.wensimin.message.databinding.TopicItemBinding
import github.wensimin.message.manager.RestApi
import github.wensimin.message.pojo.Topic
import github.wensimin.message.utils.logD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.springframework.http.HttpMethod

class TopicListAdapter(private val context: MainActivity, items: List<Topic> = arrayListOf()) :
    ArrayAdapter<Topic>(context, 0, items) {

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // 下行register调用类型推断有问题,这里使用临时变量
        val topicData = getItem(position)
        val dataBind = TopicItemBinding.inflate(
            LayoutInflater.from(parent.context)
        ).apply {
            topic = topicData
            register.setOnClickListener {
                topicData?.let {
                    register(topicData)
                }
            }
        }
        return dataBind.root
    }

    private fun register(topic: Topic) {
        context.scope.launch(Dispatchers.IO) {
            val action = if (topic.subscribed) HttpMethod.DELETE else HttpMethod.PUT
            val res = RestApi.exchange(
                "topic/${topic.id}",
                action,
                null,
                String::class.java
            )
            logD("$action ${topic.id} ${res.error ?: "success"}")
            context.loadData()
        }
    }

}