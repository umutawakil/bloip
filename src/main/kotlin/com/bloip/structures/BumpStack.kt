package com.bloip.structures

import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Usman Mutawakil on 9/1/22.
 */

@Suppress("UNCHECKED_CAST")
class BumpStack<K, V> {
    class Node<K, V> {
        var head: Node<K, V>? = null
        var tail: Node<K, V>? = null
        var element: V
        val key: K

        constructor(head: Node<K, V>?, tail: Node<K, V>?, element: V, key: K) {
            this.head    = head
            this.tail    = tail
            this.element = element
            this.key     = key
        }

        override fun equals(other:  Any?) : Boolean {
            if (other == null || this.key == null) {
                return false
            }
            val otherObject: Node<K, V> = other as Node<K, V>
            return this.key == otherObject.key
        }

        override fun hashCode() : Int {
            return this.key.hashCode()
        }
    }

    private var headNode: Node<K,V>? = null
    private val map:MutableMap<K, Node<K, V>> = ConcurrentHashMap<K, Node<K, V>>()
    private var size: Int = 0

    class Page<K, V> {
        val previousOffsetKey: K?
        val nextOffsetKey: K?
        val values: List<V>

        constructor(previousOffsetKey: K?, nextOffsetKey: K?, values: List<V>) {
            this.previousOffsetKey = previousOffsetKey
            this.nextOffsetKey     = nextOffsetKey
            this.values          = values
        }
    }

    fun push(key:K, element:V) {
        if (map.containsKey(key)) { //You can only add elements to a bump stack once.
            throw RuntimeException("Duplicate stack entries for key: $key")
        }

        val newNode = Node(
            head = null,
            tail = this.headNode,
            element = element,
            key = key
        )

        this.headNode?.head = newNode
        this.headNode = newNode
        map[key] = newNode
        size++
    }

    fun bump(key: K) {
        val node: Node<K, V> = map[key] ?: return
        if (node == this.headNode) {
            return
        }

        node.tail?.head = node.head
        node.head?.tail = node.tail

        this.headNode?.head = node
        node.head = null
        node.tail = this.headNode
        this.headNode = node
    }

    //At the moment there is no reason two threads would try to remove the same element
    fun remove(key: K) : Node<K,V>? {
        val node: Node<K, V> = map[key] ?: return null

        if (node == this.headNode) {
            node.tail?.head = null
            this.headNode = node.tail
            node.tail = null

        } else {
            node.tail?.head = node.head
            node.head?.tail = node.tail
        }
        map.remove(key)
        size--
        return node
    }

    fun update(key: K, value: V) {
        val node: Node<K, V> = map[key]?: throw RuntimeException("Trying to update key that does not exist(key: $key, v: $value)")
        node.element = value
    }

    fun get(key: K) : V? {
        return map[key]?.element
    }

    fun size() : Int {
        return this.size
    }

    fun nextPage(inputKey: K?, N: Int) : Page<K, V> {
        val tempList: MutableList<V> = mutableListOf()
        var key = inputKey ?: this.headNode?.key //If no offset key is specified assume where starting from top
        var firstNode: Node<K, V>? = null
        var lastNode: Node<K, V>? = null

        for (count in 0 until N) {
            if (key == null) {
                break
            }
            val node: Node<K, V> = map[key] ?: return Page(null, null, emptyList())

            if (firstNode == null) {
                firstNode = node
            }
            lastNode = node

            tempList.add(node.element)
            key = node.tail?.key
        }
        return Page(previousOffsetKey = firstNode?.head?.key, nextOffsetKey = lastNode?.tail?.key, tempList)
    }

    fun previousPage(inputKey: K, N: Int) : Page<K, V> {
        val tempList = LinkedList<V>()
        var key: K? = inputKey
        var firstNode: Node<K, V>? = null
        var lastNode: Node<K, V>? = null


        for (count in 0 until N) {
            if (key == null) {
                break
            }
            val node: Node<K, V> = map[key]
                ?: return Page(
                    null,
                    null,
                    emptyList()
                ) //TODO: Why does the map expect to return a null despite no ? in definition?

            if (firstNode == null) {
                firstNode = node
            }
            lastNode = node
            tempList.push(node.element)
            key = node.head?.key
        }
        return Page(previousOffsetKey = lastNode?.head?.key, nextOffsetKey = firstNode?.tail?.key, tempList)
    }

    fun getAll() : List<V> {
        val elements: MutableList<V> = mutableListOf()
        var node: Node<K, V>? = this.headNode

        while (node != null) {
            elements.add(node.element)
            node = node.tail
        }
        return elements
    }
}