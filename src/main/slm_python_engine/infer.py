import sys
import os
import json
import torch
from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel
from json_reader import get_volunteer_field

# Paths & Base Model Configuration
ADAPTER_DIR = "./slm_lora_adapter"
BASE_MODEL = "Qwen/Qwen2.5-0.5B-Instruct"

def load_inference_model():
    """Loads base model and attaches the fine-tuned LoRA adapter weights."""
    print("Loading tokenizer and model for inference...", file=sys.stderr)
    tokenizer = AutoTokenizer.from_pretrained(BASE_MODEL, trust_remote_code=True)

    device = "cuda" if torch.cuda.is_available() else "cpu"
    torch_dtype = torch.float16 if torch.cuda.is_available() else torch.float32

    base_model = AutoModelForCausalLM.from_pretrained(
        BASE_MODEL,
        torch_dtype=torch_dtype,
        device_map="auto" if torch.cuda.is_available() else None,
        trust_remote_code=True
    )

    if os.path.exists(ADAPTER_DIR):
        print(f"Applying fine-tuned LoRA weights from '{ADAPTER_DIR}'...", file=sys.stderr)
        model = PeftModel.from_pretrained(base_model, ADAPTER_DIR)
    else:
        print("Warning: Fine-tuned adapter not found. Using base model.", file=sys.stderr)
        model = base_model

    return model, tokenizer

def run_inference(user_prompt):
    """Executes SLM intent parsing, extracts parameters, and queries json_reader."""
    model, tokenizer = load_inference_model()

    messages = [
        {
            "role": "system",
            "content": (
                "You are an AI assistant for the AYLUS volunteer management system. "
                "Output strictly a JSON object with the format: "
                "{\"action\": \"get_volunteer_data\", \"parameters\": {\"name\": \"...\", \"branch\": \"...\", \"field\": \"...\"}}."
            )
        },
        {"role": "user", "content": user_prompt}
    ]

    # Format using ChatML template
    prompt = tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    inputs = tokenizer(prompt, return_tensors="pt").to(model.device)

    with torch.no_grad():
        outputs = model.generate(
            **inputs,
            max_new_tokens=128,
            temperature=0.01,  # Low temperature for deterministic output formatting
            do_sample=False
        )

    decoded = tokenizer.decode(outputs[0], skip_special_tokens=True)

    try:
        # Isolate and parse the assistant's generated JSON payload
        assistant_text = decoded.split("assistant")[-1].strip()
        start_idx = assistant_text.find("{")
        end_idx = assistant_text.rfind("}") + 1
        json_str = assistant_text[start_idx:end_idx]

        parsed_call = json.loads(json_str)
        params = parsed_call.get("parameters", {})
        name = params.get("name")
        branch = params.get("branch")
        field = params.get("field", "totalHours")

        # Execute deterministic data retrieval via json_reader.py
        # Adjust relative path to json_data_storage based on execution context
        storage_path = "../../../json_data_storage" if os.path.exists("../../../json_data_storage") else "json_data_storage"
        result = get_volunteer_field(name, branch, field, json_dir=storage_path)

        # Print final formatted response string to stdout for Java capture
        print(f"{name} has {result} for '{field}' in the {branch} branch.")

    except Exception as e:
        print(f"Error parsing model tool call: {str(e)} | Raw Output: {decoded}")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        query_text = sys.argv[1]
        run_inference(query_text)
    else:
        print("Error: No query argument provided.")