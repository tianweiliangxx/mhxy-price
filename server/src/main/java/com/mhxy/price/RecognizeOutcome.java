package com.mhxy.price;

import java.util.List;

public record RecognizeOutcome(List<RecognizedItem> items, String reasoning) {}
