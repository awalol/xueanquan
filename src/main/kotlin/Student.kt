import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLDecoder

class Student(private val username: String, private val password: String) {
    private val client = OkHttpClient()
    private var cookie = ""
    private var serverSideUrl: String = ""
    private var specialId = 0
    private lateinit var userInfo: JSONObject
    private lateinit var specialInfo: JSONObject

    fun start() {
        try {
            login(username, password)
            val homeworkList = getHomeworkList()
            for (homework in homeworkList) {
                if (homework !is JSONObject) {
                    continue
                }
                if (homework.getString("sort") == "Skill") {
                    continue
                }
                if (homework.getString("workStatus").equals("UnFinish")) { // 获取未完成列表
                    specialId = getSpecialId(homework.getString("linkUrl"))
                    println(specialId)
                    userInfo = getUserInfo()
                    specialInfo = getSpecialInfo(specialId)
                    println(specialInfo)
                    stepStatus()
                    sign()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun login(username: String, password: String) {
        val json = JSONObject()
        json["username"] = username
        json["password"] = password
        json["loginOrigin"] = 1
        val body = json.toJSONString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://appapi.xueanquan.com/usercenter/api/v3/wx/login?checkShowQrCode=true&tmp=false")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val loginInfo = JSON.parseObject(response.body!!.string())
            if (loginInfo.getInteger("err_code") != 0) {
                throw Exception("$username | ${loginInfo.getString("err_desc")}")
            } else {
                cookie = getCookie(response.headers("Set-Cookie"))
                // 这两个接口都能用
                serverSideUrl = getServiceSideUrl(loginInfo.getJSONObject("data").getString("serverSide"))
//                serverSideUrl = loginInfo.getJSONObject("data").getString("serverSide") + "Topic/topic/platformapi/api/v1"
                println(serverSideUrl)
            }
        }
    }

    private fun getNoFinishHomeworkCount(): Int {
        val request = Request.Builder()
            .url("https://yyapi.xueanquan.com/guangxi/api/v1/StudentHomeWork/NoFinishHomeWorkCount")
            .addHeader("Cookie", cookie)
            .build()

        client.newCall(request).execute().use { response ->
            return JSON.parseObject(response.body!!.string()).getInteger("result")
        }
    }

    private fun getUserInfo(): JSONObject {
        val request = Request.Builder()
            .url("$serverSideUrl/users/user-info")
            .addHeader("Cookie", cookie)
            .build()

        client.newCall(request).execute().use { response ->
            return JSON.parseObject(response.body!!.string())
        }
    }

    private fun getCookie(cookies: List<String>): String {
        var cookie = ""
        cookies.forEach {
            cookie += URLDecoder.decode(it.substring(0, it.indexOf(";") + 1))
        }
        return cookie
    }

    private fun sign() {
        val json = JSONObject()
        val request = Request.Builder()
            .url("$serverSideUrl/records/sign")
            .addHeader("Cookie",cookie)
            .addHeader("Referer", "https://huodong.xueanquan.com/")
        json["specialId"] = specialId

        //step 1
        json["step"] = 1
        val body1 = json.toJSONString().toRequestBody("application/json".toMediaType())
        request.post(body1)
        client.newCall(request.build()).execute().use { response ->
            println(response.body!!.string())
        }

        stepStatus() // 不加这玩意好像不会更新状态


        val optionalGrades = specialInfo.getJSONObject("data").getJSONArray("optionalGrades").toArray()

        for (item in optionalGrades) {
            if (item != userInfo.getInteger("grade")) {
                continue
            }
            //step 2
            json["step"] = 2
            json["countyId"] = userInfo.getInteger("countyId")
            println(json.toJSONString())
            val body2 = json.toJSONString().toRequestBody("application/json".toMediaType())
            request.post(body2)
            client.newCall(request.build()).execute().use { response ->
                println(response.body!!.string())
            }

            stepStatus()
        }
    }

    private fun stepStatus() {
        val request = Request.Builder()
            .url("$serverSideUrl/records/finish-status?specialId=$specialId")
            .addHeader("Cookie",cookie)
            .build()
        client.newCall(request).execute().use { response ->
            println(response.body!!.string())
        }
    }

    private fun getHomeworkList(): JSONArray {
        val request = Request.Builder()
            .url("https://yyapi.xueanquan.com/guangxi/safeapph5/api/v1/homework/homeworklist")
            .addHeader("Cookie", cookie)
            .build()

        client.newCall(request).execute().use { response ->
            return JSON.parseArray(response.body!!.string())
        }
    }

    private fun getServiceSideUrl(serverSide: String): String {
        val request = Request.Builder()
            .url("https://huodongapi.xueanquan.com/Topic/topic/main/api/v1/users/get-serviceside?serviceSide=$serverSide")
            .build()
        return "https:${
            client.newCall(request).execute().body!!.string().replace("\"", "")
        }/Topic/topic/platformapi/api/v1"
    }

    private fun getSpecialId(url: String): Int {
        println(url)
        val request = Request.Builder()
            .url(url.replace("index.html", "message.html"))
            .build()
        val source = client.newCall(request).execute().body!!.string()
        return "(?<=title>).*(?=</title)".toRegex().find(source)!!.value.toInt()
    }

    private fun getSpecialInfo(specialId: Int): JSONObject {
        val request = Request.Builder()
            .url(
                "https://huodongapi.xueanquan.com/Topic/topic/main/api/v1/records/special-info?specialId=$specialId&comeFrom=${userInfo.getInteger("comeFrom")}&countyId=${userInfo.getInteger("countyId")}"
            )
            .addHeader("Cookie", cookie)
            .build()
        client.newCall(request).execute().use { response ->
            return JSON.parseObject(response.body!!.string())
        }
    }
}