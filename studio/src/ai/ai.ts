import { Client } from '@urql/core'

export class Ai {
  private readonly client: Client

  constructor(client: Client) {
    this.client = client
  }

  /* generate text responses */
  async chat(prompt: string): Promise<string> {
    const query = `
            query AiChatQuery ( $prompt: String) { 
             aiChat( prompt : $prompt ) 
            }
     `
    const result = await this.client.query(query, {
      prompt: prompt
    })
    return (await result['data']['aiChat']) as string
  }

  /** renders images given a prompt */
  render(prompt: string): string {
    return ''
  }

  /**
   * i think we'd have a little drag-and-drop panel in the AiClient where we'd be allowed to drop {@code .mp3} or
   * {@code .mp4} or {@code .wav} files,
   * */
  transcribe(path: string): string {
    return ''
  }
}
