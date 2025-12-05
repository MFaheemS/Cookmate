package com.fast.smdproject

import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

class VolleyMultipartRequest(
    method: Int,
    url: String,
    private val mListener: Response.Listener<NetworkResponse>,
    private val mErrorListener: Response.ErrorListener
) : Request<NetworkResponse>(method, url, mErrorListener) {

    private val twoHyphens = "--"
    private val lineEnd = "\r\n"
    private val boundary = "apiclient-" + System.currentTimeMillis()

    private val mHeaders = HashMap<String, String>()
    private val mParams = HashMap<String, String>()
    private val mByteData = HashMap<String, DataPart>()

    fun addHeader(key: String, value: String) { mHeaders[key] = value }
    fun addStringParam(key: String, value: String) { mParams[key] = value }
    fun addDataParam(key: String, data: ByteArray, name: String) {
        mByteData[key] = DataPart(name, data)
    }

    override fun getHeaders(): MutableMap<String, String> = mHeaders

    override fun getBodyContentType(): String {
        return "multipart/form-data;boundary=$boundary"
    }

    override fun getBody(): ByteArray? {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        try {
            // Populate Text Params
            for ((key, value) in mParams) {
                buildTextPart(dos, key, value)
            }
            // Populate File Params
            for ((key, dataPart) in mByteData) {
                buildDataPart(dos, dataPart, key)
            }
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)
            return bos.toByteArray()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun buildTextPart(dos: DataOutputStream, parameterName: String, parameterValue: String) {
        dos.writeBytes(twoHyphens + boundary + lineEnd)
        dos.writeBytes("Content-Disposition: form-data; name=\"$parameterName\"$lineEnd")
        dos.writeBytes(lineEnd)
        dos.writeBytes(parameterValue + lineEnd)
    }

    private fun buildDataPart(dos: DataOutputStream, dataFile: DataPart, inputName: String) {
        dos.writeBytes(twoHyphens + boundary + lineEnd)
        dos.writeBytes("Content-Disposition: form-data; name=\"$inputName\"; filename=\"${dataFile.fileName}\"$lineEnd")
        dos.writeBytes("Content-Type: image/jpeg$lineEnd") // Assuming JPEGs for simplicity
        dos.writeBytes(lineEnd)
        val fileInputStream = ByteArrayInputStream(dataFile.content)
        var bytesAvailable = fileInputStream.available()
        val maxBufferSize = 1024 * 1024
        var bufferSize = Math.min(bytesAvailable, maxBufferSize)
        val buffer = ByteArray(bufferSize)
        var bytesRead = fileInputStream.read(buffer, 0, bufferSize)

        while (bytesRead > 0) {
            dos.write(buffer, 0, bufferSize)
            bytesAvailable = fileInputStream.available()
            bufferSize = Math.min(bytesAvailable, maxBufferSize)
            bytesRead = fileInputStream.read(buffer, 0, bufferSize)
        }
        dos.writeBytes(lineEnd)
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<NetworkResponse> {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response))
    }

    override fun deliverResponse(response: NetworkResponse) {
        mListener.onResponse(response)
    }

    class DataPart(val fileName: String, val content: ByteArray)
}