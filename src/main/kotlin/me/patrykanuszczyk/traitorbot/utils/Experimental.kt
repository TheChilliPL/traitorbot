package me.patrykanuszczyk.traitorbot.utils

/**
 * Implies that the annotated element is a part of the experimental API
 * and it may get changed or removed without any notice,
 * even between patch versions.
 */
@Retention(AnnotationRetention.SOURCE)
annotation class Experimental