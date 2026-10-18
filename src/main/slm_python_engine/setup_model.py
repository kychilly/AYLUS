import torch
from transformers import AutoTokenizer, AutoModelForCausalLM

# Choose base model (Qwen2.5-0.5B-Instruct or HuggingFaceTB/SmolLM2-135M-Instruct)
MODEL_NAME = "Qwen/Qwen2.5-0.5B-Instruct"

def load_tokenizer_and_model(model_name=MODEL_NAME):
    """
    Loads the pre-trained tokenizer and base architecture.
    Configures special tokens and padding conventions for structured JSON generation.
    """
    print(f"Loading tokenizer and model base architecture for '{model_name}'...")

    # 1. Load Tokenizer
    tokenizer = AutoTokenizer.from_pretrained(
        model_name,
        trust_remote_code=True
    )

    # 2. Ensure Padding Token is Set Correctly
    if tokenizer.pad_token is None:
        tokenizer.pad_token = tokenizer.eos_token
        tokenizer.pad_token_id = tokenizer.eos_token_id

    tokenizer.padding_side = "right"  # Optimal for causal language model training

    # 3. Load Model Base Architecture
    device = "cuda" if torch.cuda.is_available() else "cpu"
    model = AutoModelForCausalLM.from_pretrained(
        model_name,
        torch_dtype=torch.float16 if torch.cuda.is_available() else torch.float32,
        device_map="auto" if torch.cuda.is_available() else None,
        trust_remote_code=True
    )

    print(f"Model successfully loaded on device: {device}")
    return tokenizer, model

def test_chat_template(tokenizer):
    """
    Tests ChatML template formatting on a sample volunteer prompt.
    """
    sample_messages = [
        {
            "role": "system",
            "content": "You are an AI assistant for the AYLUS volunteer management system."
        },
        {
            "role": "user",
            "content": "How many total hours does Ellie Tandlmayer have in the Albany branch?"
        }
    ]

    # Render formatted prompt using the model's native chat template
    formatted_prompt = tokenizer.apply_chat_template(
        sample_messages,
        tokenize=False,
        add_generation_prompt=True
    )

    print("\n--- Formatted ChatML Prompt Verification ---")
    print(formatted_prompt)
    print("-------------------------------------------\n")

if __name__ == "__main__":
    tokenizer, model = load_tokenizer_and_model()
    test_chat_template(tokenizer)